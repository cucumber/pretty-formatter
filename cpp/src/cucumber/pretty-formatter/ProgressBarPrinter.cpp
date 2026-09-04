#include "cucumber/pretty-formatter/ProgressBarPrinter.hpp"
#include "cucumber/messages/Duration.hpp"
#include "cucumber/messages/DurationUtil.hpp"
#include "cucumber/messages/Envelope.hpp"
#include "cucumber/messages/TestCase.hpp"
#include "cucumber/messages/TestCaseFinished.hpp"
#include "cucumber/messages/TestCaseStarted.hpp"
#include "cucumber/messages/TestRunFinished.hpp"
#include "cucumber/messages/TestRunHookFinished.hpp"
#include "cucumber/messages/TestStepFinished.hpp"
#include "cucumber/messages/TestStepResultStatus.hpp"
#include "cucumber/messages/UndefinedParameterType.hpp"
#include "cucumber/pretty-formatter/Ansi.hpp"
#include "cucumber/pretty-formatter/CaseUtil.hpp"
#include "cucumber/pretty-formatter/ExceptionFormatter.hpp"
#include "cucumber/pretty-formatter/FormatDuration.hpp"
#include "cucumber/pretty-formatter/FormatSnippets.hpp"
#include "cucumber/pretty-formatter/GroupBy.hpp"
#include "cucumber/pretty-formatter/LineBuilder.hpp"
#include "cucumber/pretty-formatter/SourceReferenceFormatter.hpp"
#include "cucumber/pretty-formatter/Statuses.hpp"
#include "cucumber/pretty-formatter/StepFormatter.hpp"
#include "cucumber/pretty-formatter/TestRunHookFormatter.hpp"
#include "cucumber/pretty-formatter/Theme.hpp"
#include "cucumber/query/Query.hpp"
#include <algorithm>
#include <cmath>
#include <cstddef>
#include <cstdint>
#include <fmt/core.h>
#include <fmt/format.h>
#include <fmt/ostream.h>
#include <functional>
#include <iterator>
#include <map>
#include <memory>
#include <numeric>
#include <ostream>
#include <set>
#include <sstream>
#include <string>
#include <string_view>
#include <utility>
#include <vector>

namespace cucumber::pretty_formatter
{
    namespace
    {
        enum class ProblemType : std::uint8_t
        {
            parameter,
            globalHook,
            testCase,
            testRun,
        };

        std::string FormatProblem(ProblemType type, std::string_view message)
        {
            std::string_view typeStr = "Unknown problem";

            switch (type)
            {
                case ProblemType::parameter:
                    typeStr = "Undefined parameter type:";
                    break;

                case ProblemType::globalHook:
                    typeStr = "Global hook:";
                    break;

                case ProblemType::testCase:
                    typeStr = "Scenario:";
                    break;

                case ProblemType::testRun:
                    typeStr = "Test run:";
                    break;
            }

            return fmt::format("{}{}{} {}", Ansi{ Ansi::Attribute::italic }.ToString(), typeStr,
                Ansi{ Ansi::Attribute::italicOff }.ToString(), message);
        }

        std::string IndentNumbered(std::size_t indent, std::size_t number, const std::string& message)
        {
            std::string indentStr(indent, ' ');

            std::istringstream istream{ message };
            std::vector<std::string> lines;
            bool first = true;
            for (std::string line; std::getline(istream, line);)
            {
                if (line == "")
                {
                    lines.emplace_back("");
                }
                else
                {
                    if (first)
                    {
                        first = false;
                        lines.emplace_back(fmt::format("{}{}) {}", indentStr, number, line));
                    }
                    else
                    {
                        lines.emplace_back(fmt::format("{}   {}", indentStr, line));
                    }
                }
            }

            return fmt::format("{}", fmt::join(lines, "\n"));
        }

        std::string FormatCounts(std::string_view singular, std::string_view plural,
            const std::map<messages::TestStepResultStatus, std::size_t>& counts, Theme& theme)
        {
            const auto total = std::accumulate(counts.begin(), counts.end(), 0U,
                [&](std::size_t total, const auto& pair)
                {
                    const auto& [status, count] = pair;
                    return total + count;
                });

            std::string result = fmt::format("{} {}", total, total == 1 ? singular : plural);

            if (total != 0)
            {
                bool first = true;
                result += " (";
                for (const auto& status : allStatuses)
                {
                    if (counts.find(status) != counts.end())
                    {
                        if (!first)
                        {
                            result += ", ";
                        }
                        result += theme.Style(Theme::Element::step, status,
                            fmt::format("{} {}", counts.at(status), ToLower(messages::to_string(status))));
                        first = false;
                    }
                }
                result += ")";
            }

            return result;
        }

        std::string MakeStats(const query::Query& query, const std::shared_ptr<Theme>& theme)
        {
            std::vector<std::string> lines;

            {
                const auto& optTestRunFinished = query.FindTestRunFinished();
                if (optTestRunFinished.has_value() && optTestRunFinished.value()->exception.has_value())
                {
                    lines.emplace_back("");
                    lines.emplace_back(FormatCounts("test run", "test runs", { { messages::TestStepResultStatus::FAILED, 1 } }, *theme));
                }
            }

            {
                const auto& allTestRunHookFinished = query.FindAllTestRunHookFinished();
                if (!allTestRunHookFinished.empty())
                {
                    const auto& hooksGroupedByStatus = GroupBy(
                        [](const std::shared_ptr<const messages::TestRunHookFinished>& testRunHookFinished)
                        {
                            return testRunHookFinished->result->status;
                        },
                        allTestRunHookFinished);

                    std::map<messages::TestStepResultStatus, std::size_t> hookCountGroupedByStatus;
                    std::transform(hooksGroupedByStatus.begin(), hooksGroupedByStatus.end(),
                        std::inserter(hookCountGroupedByStatus, hookCountGroupedByStatus.end()),
                        [](const auto& pair)
                        {
                            return std::make_pair(pair.first, pair.second.size());
                        });

                    lines.emplace_back(FormatCounts("hook", "hooks", hookCountGroupedByStatus, *theme));
                }
            }

            {
                std::map<messages::TestStepResultStatus, std::size_t> testCaseCountGroupedByStatus;
                const auto& allTestCaseFinished = query.FindAllTestCaseFinished();
                for (const auto& testCaseFinished : allTestCaseFinished)
                {
                    const auto& optMostSevereTestStepResult = query.FindMostSevereTestStepResultBy(testCaseFinished);
                    if (optMostSevereTestStepResult.has_value())
                    {
                        const auto& mostSevereTestStepResult = optMostSevereTestStepResult.value();
                        testCaseCountGroupedByStatus[mostSevereTestStepResult->status]++;
                    }
                    else
                    {
                        testCaseCountGroupedByStatus[messages::TestStepResultStatus::PASSED]++;
                    }
                }

                lines.emplace_back(FormatCounts("scenario", "scenarios", testCaseCountGroupedByStatus, *theme));
            }

            {
                std::map<messages::TestStepResultStatus, std::size_t> stepCountGroupedByStatus;
                const auto& allTestCaseFinished = query.FindAllTestCaseFinished();
                for (const auto& testCaseFinished : allTestCaseFinished)
                {
                    const auto& testStepsFinished = query.FindTestStepsFinishedBy(testCaseFinished);
                    for (const auto& testStepFinished : testStepsFinished)
                    {
                        stepCountGroupedByStatus[testStepFinished->testStepResult->status]++;
                    }
                }

                lines.emplace_back(FormatCounts("step", "steps", stepCountGroupedByStatus, *theme));
            }

            {
                const auto& optTestRunDuration = query.FindTestRunDuration();
                if (optTestRunDuration.has_value())
                {
                    const auto& allTestRunHookFinished = query.FindAllTestRunHookFinished();
                    const auto testRunHookDuration =
                        std::accumulate(allTestRunHookFinished.begin(), allTestRunHookFinished.end(), messages::Duration{},
                            [](const auto& total, const std::shared_ptr<const messages::TestRunHookFinished>& testRunHookFinished)
                            {
                                return total + *testRunHookFinished->result->duration;
                            });

                    const auto& allTestStepFinished = query.FindAllTestStepFinished();
                    const auto testStepDuration =
                        std::accumulate(allTestStepFinished.begin(), allTestStepFinished.end(), messages::Duration{},
                            [](const auto& total, const std::shared_ptr<const messages::TestStepFinished>& testStepFinished)
                            {
                                return total + *testStepFinished->testStepResult->duration;
                            });

                    lines.emplace_back(fmt::format("{} ({} executing your code)", FormatDuration(optTestRunDuration.value()),
                        FormatDuration(std::make_shared<messages::Duration>(testRunHookDuration + testStepDuration))));
                }
            }

            return fmt::format("{}", fmt::join(lines, "\n"));
        }
    }

    ProgressBarPrinter::TtyOstream::TtyOstream(std::ostream& stream)
        : stream{ stream }
    {}

    void ProgressBarPrinter::TtyOstream::Write(std::string_view output)
    {
        fmt::print(stream, "{}", output);
        stream.flush();
    }

    void ProgressBarPrinter::TtyOstream::ClearScreenDown()
    {
        fmt::print(stream, "\x1b[0J");
    }

    void ProgressBarPrinter::TtyOstream::MoveCursorUp(std::size_t lines)
    {
        fmt::print(stream, "\r\x1b[{}A", lines);
    }

    struct ProgressBarPrinter::Data : query::Query
    {
        enum class Phase : std::uint8_t
        {
            preparing,
            running,
            done,
        };

        void Update(const cucumber::messages::Envelope& envelope)
        {
            query::Query::Update(envelope);

            if (envelope.testCase.has_value())
            {
                TestCase(envelope.testCase.value());
            }

            if (envelope.testCaseStarted.has_value())
            {
                TestCaseStarted();
            }

            if (envelope.testCaseFinished.has_value())
            {
                TestCaseFinished(envelope.testCaseFinished.value());
            }

            if (envelope.testStepFinished.has_value())
            {
                TestStepFinished();
            }

            if (envelope.testRunFinished.has_value())
            {
                TestRunFinished();
            }
        }

        [[nodiscard]] Phase Phase() const
        {
            return phase;
        }

        [[nodiscard]] std::size_t TotalScenarios() const
        {
            return totalScenarios;
        }

        [[nodiscard]] std::size_t TotalSteps() const
        {
            return totalSteps;
        }

        [[nodiscard]] std::size_t FinishedScenarios() const
        {
            return finishedScenarios;
        }

        [[nodiscard]] std::size_t FinishedSteps() const
        {
            return finishedSteps;
        }

        [[nodiscard]] std::size_t RunningScenarios() const
        {
            return runningScenarios;
        }

    private:
        void TestCase(const std::shared_ptr<const messages::TestCase>& testCase)
        {
            ++totalScenarios;
            totalSteps += testCase->testSteps.size();
        }

        void TestCaseStarted()
        {
            ++runningScenarios;
            phase = Phase::running;
        }

        void TestCaseFinished(const std::shared_ptr<const messages::TestCaseFinished>& testCaseFinished)
        {
            --runningScenarios;
            ++finishedScenarios;

            if (testCaseFinished->willBeRetried)
            {
                const auto& optTestCase = FindTestCaseBy(testCaseFinished);
                if (optTestCase.has_value())
                {
                    --finishedScenarios;
                    finishedSteps -= optTestCase.value()->testSteps.size();
                }
            }
        }

        void TestStepFinished()
        {
            ++finishedSteps;
        }

        void TestRunFinished()
        {
            phase = Phase::done;
        }

        enum Phase phase
        {
            Phase::preparing
        };

        std::size_t totalScenarios{ 0 };
        std::size_t totalSteps{ 0 };

        std::size_t finishedScenarios{ 0 };
        std::size_t finishedSteps{ 0 };

        std::size_t runningScenarios{ 0 };
    };

    struct ProgressBarPrinter::Printer
    {
        Printer(Tty& tty, Data& data, std::shared_ptr<Theme> theme, std::size_t maxWidth,
            std::function<std::string(std::string)> uriFormatter, std::set<enum Options> options)
            : tty{ tty }
            , data{ data }
            , theme{ theme }
            , maxWidth{ maxWidth }
            , uriFormatter{ std::move(uriFormatter) }
            , stepFormatter{ data, theme, sourceReferenceFormatter, stepIndent, options.find(Options::includeAttachments) != options.end() }
            , testRunHookFormatter{ data, std::move(theme), sourceReferenceFormatter, stepIndent }
        {}

        ~Printer() = default;

        Printer(const Printer&) = delete;
        Printer& operator=(const Printer&) = delete;

        Printer(Printer&&) = delete;
        Printer& operator=(Printer&&) = delete;

        void UndefinedParameterType(const std::shared_ptr<const messages::UndefinedParameterType>& undefinedParameterType)
        {
            pendingProblems.emplace_back(ProblemType::parameter,
                fmt::format("'{}' in '{}'", undefinedParameterType->name, undefinedParameterType->expression));
        }

        void TestRunStarted()
        {
            ReRender(true);
        }

        void TestCase()
        {
            ReRender();
        }

        void TestRunHookFinished(const std::shared_ptr<const messages::TestRunHookFinished>& testRunHookFinished)
        {
            if (failingStatuses.find(testRunHookFinished->result->status) != failingStatuses.end())
            {
                pendingProblems.emplace_back(ProblemType::globalHook, FormatGlobalHookProblem(testRunHookFinished));
            }

            ReRender();
        }

        void TestCaseStarted(const std::shared_ptr<const messages::TestCaseStarted>& testCaseStarted)
        {
            ReRender();
        }

        void TestStepFinished(const std::shared_ptr<const messages::TestStepFinished>& testStepFinished)
        {
            ReRender();
        }

        void TestCaseFinished(const std::shared_ptr<const messages::TestCaseFinished>& testCaseFinished)
        {
            if (!testCaseFinished->willBeRetried)
            {
                const auto& optMostSevereTestStepResult = data.FindMostSevereTestStepResultBy(testCaseFinished);
                if (optMostSevereTestStepResult.has_value() &&
                    failingStatuses.find(optMostSevereTestStepResult.value()->status) != failingStatuses.end())
                {
                    pendingProblems.emplace_back(ProblemType::testCase, FormatTestCaseProblem(testCaseFinished));
                }
            }
            ReRender();
        }

        void TestRunFinished(const std::shared_ptr<const messages::TestRunFinished>& testRunFinished)
        {
            if (testRunFinished->exception.has_value())
            {
                ExceptionFormatter exceptionFormatter{ 4, theme, messages::TestStepResultStatus::FAILED };
                const auto& optFormattedException = exceptionFormatter.Format(testRunFinished->exception.value());
                if (optFormattedException.has_value())
                {
                    pendingProblems.emplace_back(ProblemType::testRun, "\n" + optFormattedException.value());
                }
            }

            ReRender();
        }

    private:
        std::string FormatGlobalHookProblem(const std::shared_ptr<const messages::TestRunHookFinished>& testRunHookFinished)
        {
            LineBuilder lineBuilder{ theme };

            lineBuilder.Accept(
                [this, &testRunHookFinished](LineBuilder& lineBuilder)
                {
                    testRunHookFormatter.FormatHookLineTo(lineBuilder, testRunHookFinished);
                });

            const auto exception = testRunHookFormatter.FormatException(testRunHookFinished);
            if (!exception.empty())
            {
                lineBuilder.NewLine().Append(exception);
            }

            return lineBuilder.Build();
        }

        std::string FormatTestCaseProblem(const std::shared_ptr<const messages::TestCaseFinished>& testCaseFinished)
        {
            LineBuilder lineBuilder{ theme };

            const auto& optTestCaseStarted = data.FindTestCaseStartedBy(testCaseFinished);
            if (optTestCaseStarted.has_value())
            {
                const auto& testCaseStarted = optTestCaseStarted.value();
                const auto& optPickle = data.FindPickleBy(testCaseStarted);
                if (optPickle.has_value())
                {
                    const auto& pickle = optPickle.value();
                    lineBuilder.Append(pickle->name)
                        .Accept(
                            [&testCaseStarted](LineBuilder& lineBuilder)
                            {
                                if (testCaseStarted->attempt > 0)
                                {
                                    lineBuilder.Append(", after " + std::to_string(testCaseStarted->attempt + 1) + " attempts");
                                }
                            })
                        .Append(" ")
                        .Append(Theme::Element::location, "# " + sourceReferenceFormatter.Format(pickle->uri, data.FindLocationOf(pickle)));
                }
            }

            const auto steps = stepFormatter.FormatNonPassingSteps(testCaseFinished);
            if (!steps.empty())
            {
                lineBuilder.NewLine().Append(steps);
            }

            return lineBuilder.Build();
        }

        [[nodiscard]] std::string MakeRepeatedString(std::string_view str, std::size_t count) const
        {
            std::string result;
            result.reserve(str.size() * count);
            for (std::size_t i = 0; i < count; ++i)
            {
                result.append(str);
            }
            return result;
        }

        [[nodiscard]] std::string MakeBar(std::size_t finished, std::size_t total, std::string_view label) const
        {
            const auto barWidth = maxWidth;
            const auto ratio = total > 0 ? static_cast<double>(finished) / static_cast<double>(total) : 0.0;
            const auto filledCount = static_cast<std::size_t>(std::round(ratio * static_cast<double>(barWidth)));
            const auto emptyCount = barWidth - filledCount;

            auto filled = MakeRepeatedString("█", filledCount);
            auto empty = MakeRepeatedString("░", emptyCount);

            return fmt::format("{}{} {}/{} {}", filled, empty, finished, total, label);
        }

        [[nodiscard]] std::string MakeStatus() const
        {
            switch (data.Phase())
            {
                case Data::Phase::preparing:
                    return "Getting ready...";
                case Data::Phase::running:
                    return fmt::format("Running {} scenarios...", data.RunningScenarios());
                case Data::Phase::done:
                    return "Done";
            }
            return "Unknown";
        }

        [[nodiscard]] std::string MakeProgressBlock() const
        {
            return fmt::format("\n{}\n{}\n{}\n", MakeBar(data.FinishedScenarios(), data.TotalScenarios(), "scenarios"),
                MakeBar(data.FinishedSteps(), data.TotalSteps(), "steps"), MakeStatus());
        }

        [[nodiscard]] std::string MakeSummaryBlock() const
        {
            std::string summary = fmt::format("\n{}\n", MakeStats(data, theme));

            const auto& optSnippets = FormatSnippets(data);
            if (optSnippets.has_value())
            {
                summary += optSnippets.value() + "\n";
            }

            return summary;
        }

        void ReRender(bool initial = false)
        {
            std::string output;

            if (!pendingProblems.empty())
            {
                if (printedProblemCount == 0)
                {
                    output += "Problems:\n";
                }

                for (const auto& [type, problem] : std::exchange(pendingProblems, {}))
                {
                    const auto problemText = FormatProblem(type, problem);
                    ++printedProblemCount;
                    output += IndentNumbered(2, printedProblemCount, problemText) + "\n";
                }
            }

            if (data.Phase() == Data::Phase::done)
            {
                output += MakeSummaryBlock();
            }
            else
            {
                output += MakeProgressBlock();
            }

            Render(initial, output);
        }

        void Render(bool initial, std::string_view output)
        {
            if (!initial)
            {
                tty.MoveCursorUp(4);
                tty.ClearScreenDown();
            }
            tty.Write(output);
        }

        Tty& tty;
        Data& data;
        std::shared_ptr<Theme> theme;
        std::size_t maxWidth;

        std::function<std::string(std::string)> uriFormatter;

        constexpr static std::size_t stepIndent{ 2 };
        SourceReferenceFormatter sourceReferenceFormatter{ [](std::string uri)
            {
                return uri;
            } };
        StepFormatter stepFormatter;
        TestRunHookFormatter testRunHookFormatter;

        using ProblemEntry = std::pair<ProblemType, std::string>;
        std::vector<ProblemEntry> pendingProblems;
        std::size_t printedProblemCount{ 0 };
    };

    ProgressBarPrinter::ProgressBarPrinter(Tty& tty, std::shared_ptr<Theme> theme, std::size_t maxWidth,
        std::function<std::string(std::string)> uriFormatter, std::set<enum Options> options)
        : data{ std::make_unique<Data>() }
        , printer{ std::make_unique<Printer>(tty, *data, std::move(theme), maxWidth, std::move(uriFormatter), std::move(options)) }
    {}

    ProgressBarPrinter::~ProgressBarPrinter() = default;

    void ProgressBarPrinter::Update(const messages::Envelope& envelope)
    {
        data->Update(envelope);

        if (envelope.undefinedParameterType.has_value())
        {
            printer->UndefinedParameterType(envelope.undefinedParameterType.value());
        }
        if (envelope.testRunStarted.has_value())
        {
            printer->TestRunStarted();
        }
        if (envelope.testCase.has_value())
        {
            printer->TestCase();
        }
        if (envelope.testRunHookFinished.has_value())
        {
            printer->TestRunHookFinished(envelope.testRunHookFinished.value());
        }
        if (envelope.testCaseStarted.has_value())
        {
            printer->TestCaseStarted(envelope.testCaseStarted.value());
        }
        if (envelope.testStepFinished.has_value())
        {
            printer->TestStepFinished(envelope.testStepFinished.value());
        }
        if (envelope.testCaseFinished.has_value())
        {
            printer->TestCaseFinished(envelope.testCaseFinished.value());
        }
        if (envelope.testRunFinished.has_value())
        {
            printer->TestRunFinished(envelope.testRunFinished.value());
        }
    }
}
