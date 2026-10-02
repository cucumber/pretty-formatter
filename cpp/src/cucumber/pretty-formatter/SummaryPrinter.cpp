#include "cucumber/pretty-formatter/SummaryPrinter.hpp"
#include "cucumber/messages/Duration.hpp"
#include "cucumber/messages/DurationUtil.hpp"
#include "cucumber/messages/Envelope.hpp"
#include "cucumber/messages/Exception.hpp"
#include "cucumber/messages/Hook.hpp"
#include "cucumber/messages/HookType.hpp"
#include "cucumber/messages/Pickle.hpp"
#include "cucumber/messages/PickleStep.hpp"
#include "cucumber/messages/Snippet.hpp"
#include "cucumber/messages/Step.hpp"
#include "cucumber/messages/TestCaseFinished.hpp"
#include "cucumber/messages/TestCaseStarted.hpp"
#include "cucumber/messages/TestRunHookFinished.hpp"
#include "cucumber/messages/TestStep.hpp"
#include "cucumber/messages/TestStepFinished.hpp"
#include "cucumber/messages/TestStepResultStatus.hpp"
#include "cucumber/pretty-formatter/CaseUtil.hpp"
#include "cucumber/pretty-formatter/ExceptionFormatter.hpp"
#include "cucumber/pretty-formatter/FormatDuration.hpp"
#include "cucumber/pretty-formatter/Formatter.hpp"
#include "cucumber/pretty-formatter/GroupBy.hpp"
#include "cucumber/pretty-formatter/LineBuilder.hpp"
#include "cucumber/pretty-formatter/LocationComment.hpp"
#include "cucumber/pretty-formatter/SourceReferenceFormatter.hpp"
#include "cucumber/pretty-formatter/Statuses.hpp"
#include "cucumber/pretty-formatter/StepFormatter.hpp"
#include "cucumber/pretty-formatter/TestRunHookFormatter.hpp"
#include "cucumber/pretty-formatter/Theme.hpp"
#include "cucumber/query/Query.hpp"
#include "fmt/core.h"
#include "fmt/format.h"
#include "fmt/ostream.h"
#include "fmt/ranges.h"
#include <chrono>
#include <cstddef>
#include <cstdint>
#include <functional>
#include <map>
#include <memory>
#include <numeric>
#include <optional>
#include <ostream>
#include <set>
#include <string>
#include <string_view>
#include <unordered_map>
#include <unordered_set>
#include <utility>
#include <vector>

namespace cucumber::pretty_formatter
{
    namespace
    {
        template<typename T>
        const T& UnwrapReference(const T& value)
        {
            return value;
        }

        template<typename T>
        const T& UnwrapReference(const std::reference_wrapper<const T>& value)
        {
            return value.get();
        }

        std::int32_t PickleComparator(const messages::Pickle& lhs, const messages::Pickle& rhs)

        {
            if (lhs.uri != rhs.uri)
            {
                return static_cast<std::int32_t>(rhs.uri.compare(lhs.uri));
            }
            if (!lhs.location || !rhs.location)
            {
                return 0;
            }
            if (lhs.location->line != rhs.location->line)
            {
                return static_cast<std::int32_t>(lhs.location->line) - static_cast<std::int32_t>(rhs.location->line);
            }
            return static_cast<std::int32_t>(lhs.location->column.value_or(0)) -
                   static_cast<std::int32_t>(rhs.location->column.value_or(0));
        }

        template<class Container, class StatusOf>
        std::string FormatSubCounts(std::string_view singular, std::string_view plural, const Container& finishedItems, const Theme& theme,
            StatusOf&& statusOf)
        {
            const auto size = finishedItems.size();
            auto countAndName = fmt::format("{} {}", size, size == 1 ? singular : plural);

            std::map<messages::TestStepResultStatus, std::size_t> counts;
            for (const auto& item : finishedItems)
            {
                ++counts[std::invoke(std::forward<StatusOf>(statusOf), UnwrapReference(item))];
            }

            std::vector<std::string> subCounts;
            for (const auto status : allStatuses)
            {
                if (const auto count = counts[status]; count != 0)
                {
                    subCounts.push_back(
                        theme.Style(Theme::Element::step, status, fmt::format("{} {}", count, ToLower(messages::to_string(status)))));
                }
            }

            if (subCounts.empty())
            {
                return countAndName;
            }

            return fmt::format("{} ({})", countAndName, fmt::join(subCounts, ", "));
        }
    }

    struct SummaryPrinter::Data : query::Query
    {};

    struct SummaryPrinter::Printer
    {
        Printer(std::ostream& stream, Data& data, std::shared_ptr<Theme> theme, std::function<std::string(std::string)> uriFormatter,
            std::set<enum Options> options)
            : stream{ stream }
            , data{ data }
            , theme{ std::move(theme) }
            , uriFormatter{ std::move(uriFormatter) }
            , options{ std::move(options) }
        {}

        void PrintSummary()
        {
            PrintNonPassingScenarios();
            PrintUnknownParameterTypes();
            PrintNonPassingGlobalHooks();
            PrintNonPassingTestRun();
            PrintStats();
            PrintSnippets();
        }

    private:
        void FormatScenarioLineTo(const messages::TestCaseFinished& testCaseFinished, LineBuilder& lineBuilder)
        {
            const auto* testCaseStarted = data.FindTestCaseStartedBy(testCaseFinished);

            if (testCaseStarted == nullptr)
            {
                return;
            }

            const auto* pickle = data.FindPickleBy(*testCaseStarted);

            if (pickle != nullptr)
            {
                lineBuilder.Append(pickle->name)
                    .Append(FormatAttempt(*testCaseStarted))
                    .Accept(
                        [this, &pickle](LineBuilder& lineBuilder)
                        {
                            FormatLocationCommentTo(lineBuilder, *pickle);
                        });
            }
        }

        messages::TestStepResultStatus GetTestStepResultStatusByTestCaseFinished(const messages::TestCaseFinished& testCaseFinished) const
        {
            const auto* mostSevereTestStepResult = data.FindMostSevereTestStepResultBy(testCaseFinished);
            if (mostSevereTestStepResult != nullptr)
            {
                return mostSevereTestStepResult->status;
            }
            return messages::TestStepResultStatus::PASSED;
        }

        void PrintNonPassingScenarios()
        {
            const auto allTestCasesFinished = data.FindAllTestCaseFinishedOrderBy(query::findPickleByTestCaseFinished, PickleComparator);
            const auto& testCaseFinishedByStatus = GroupBy(this, &Printer::GetTestStepResultStatusByTestCaseFinished, allTestCasesFinished);

            for (const auto& status : failingStatuses)
            {
                PrintFinishedItemByStatus("scenarios", testCaseFinishedByStatus, status, &Printer::FormatScenarioLineTo,
                    &Printer::PrintNonPassingSteps);
            }
        }

        void PrintUnknownParameterTypes()
        {
            const auto undefinedParameterTypes = data.FindAllUndefinedParameterTypes();
            if (undefinedParameterTypes.empty())
            {
                return;
            }

            fmt::println(stream, "\n{}",
                theme->Style(Theme::Element::step, messages::TestStepResultStatus::UNDEFINED,
                    "These parameters are missing a parameter type definition:"));

            auto index{ 0 };
            for (const auto& undefinedParameterType : undefinedParameterTypes)
            {
                fmt::println(stream, "  {}) '{}' in '{}'", ++index, undefinedParameterType.name, undefinedParameterType.expression);
            }
        }

        void PrintNonPassingGlobalHooks()
        {
            const auto allTestRunHooksFinished = data.FindAllTestRunHookFinished();
            const auto& testRunHookFinishedByStatus =
                GroupBy(this, &Printer::GetTestStepResultStatusByTestRunHookFinished, allTestRunHooksFinished);

            for (const auto& status : failingStatuses)
            {
                PrintFinishedItemByStatus("hooks", testRunHookFinishedByStatus, status, &Printer::FormatHookLineTo,
                    &Printer::PrintTestRunHookException);
            }
        }

        void PrintNonPassingTestRun()
        {
            const auto& optException = GetTestRunWithException();
            if (optException)
            {
                fmt::println(stream, "{}",
                    theme->Style(Theme::Element::step, messages::TestStepResultStatus::FAILED,
                        SentenceCase(messages::to_string(messages::TestStepResultStatus::FAILED)) + " test run:"));

                constexpr auto indent{ 7 };
                ExceptionFormatter exceptionFormatter{ indent, theme, messages::TestStepResultStatus::FAILED };

                const auto formattedException = exceptionFormatter.Format(*optException);
                if (formattedException)
                {
                    fmt::print(stream, "{}", *formattedException);
                }
            }
        }

        void PrintStats()
        {
            fmt::println(stream, "");
            PrintTestRunCount();
            PrintGlobalHookCount();
            PrintScenarioCounts();
            PrintStepCounts();
            PrintDurations();
        }

        void PrintTestRunCount()
        {
            const auto& optException = GetTestRunWithException();
            if (optException.has_value())
            {
                fmt::println(stream, "1 test run ({})",
                    theme->Style(Theme::Element::step, messages::TestStepResultStatus::FAILED, "1 failed"));
            }
        }

        void PrintGlobalHookCount()
        {
            const auto allTestRunHookFinished = data.FindAllTestRunHookFinished();
            if (allTestRunHookFinished.empty())
            {
                return;
            }

            fmt::println(stream, "{}",
                FormatSubCounts("hook", "hooks", allTestRunHookFinished, *theme,
                    [this](const auto& item)
                    {
                        return GetTestStepResultStatusByTestRunHookFinished(item);
                    }));
        }

        void PrintScenarioCounts()
        {
            fmt::println(stream, "{}",
                FormatSubCounts("scenario", "scenarios", data.FindAllTestCaseFinished(), *theme,
                    [this](const auto& item)
                    {
                        return Printer::GetTestStepResultStatusByTestCaseFinished(item);
                    }));
        }

        void PrintStepCounts()
        {
            std::vector<std::reference_wrapper<const messages::TestStepFinished>> testStepsFinished;
            for (const auto& testCaseFinished : data.FindAllTestCaseFinished())
            {
                for (const auto& testStepFinished : data.FindTestStepsFinishedBy(testCaseFinished))
                {
                    testStepsFinished.emplace_back(testStepFinished);
                }
            }

            fmt::println(stream, "{}",
                FormatSubCounts("step", "steps", testStepsFinished, *theme,
                    [this](const messages::TestStepFinished& item)
                    {
                        return item.testStepResult.status;
                    }));
        }

        void PrintDurations()
        {
            const auto optRunDuration = data.FindTestRunDuration();
            if (optRunDuration)
            {
                fmt::println(stream, "{} ({} executing your code)", FormatDuration(*optRunDuration),
                    FormatDuration(GetExecutionDuration()));
            }
        }

        void PrintSnippets()
        {
            const auto allTestCasesFinishedOrdered =
                data.FindAllTestCaseFinishedOrderBy(query::findPickleByTestCaseFinished, PickleComparator);

            std::vector<const messages::Snippet*> snippets;
            std::unordered_set<std::string> seen;

            for (const auto& testCaseFinished : allTestCasesFinishedOrdered)
            {
                const auto* pickle = data.FindPickleBy(testCaseFinished);
                if (pickle != nullptr)
                {
                    for (const auto& suggestion : data.FindSuggestionsBy(*pickle))
                    {
                        for (const auto& snippet : suggestion.snippets)
                        {
                            if (seen.insert(snippet.language + "-" + snippet.code).second)
                            {
                                snippets.push_back(std::addressof(snippet));
                            }
                        }
                    }
                }
            }

            if (snippets.empty())
            {
                return;
            }

            fmt::println(stream, "\nYou can implement missing steps with the snippets below:\n");
            for (const auto& snippet : snippets)
            {
                fmt::println(stream, "{}\n", snippet->code);
            }
        }

        messages::TestStepResultStatus GetTestStepResultStatusByTestRunHookFinished(
            const messages::TestRunHookFinished& testRunHookFinished) const
        {
            return testRunHookFinished.result.status;
        }

        messages::TestStepResultStatus GetTestStepResultStatusByTestStepFinished(const messages::TestStepFinished& testStepFinished) const
        {
            return testStepFinished.testStepResult.status;
        }

        std::optional<messages::Exception> GetTestRunWithException() const
        {
            const auto* testRunFinished = data.FindTestRunFinished();

            if (testRunFinished != nullptr && !testRunFinished->success)
            {
                return testRunFinished->exception;
            }

            return std::nullopt;
        }

        messages::Duration GetExecutionDuration() const
        {
            const auto allTestRunHookFinished = data.FindAllTestRunHookFinished();
            const auto allTestStepFinished = data.FindAllTestStepFinished();

            const auto testRunHookFinishedDuration =
                std::accumulate(allTestRunHookFinished.begin(), allTestRunHookFinished.end(), messages::Duration{},
                    [](const messages::Duration& totalDuration, const messages::TestRunHookFinished& testRunHookFinished)
                    {
                        return totalDuration + testRunHookFinished.result.duration;
                    });

            const auto testStepFinishedDuration =
                std::accumulate(allTestStepFinished.begin(), allTestStepFinished.end(), messages::Duration{},
                    [](const messages::Duration& totalDuration, const messages::TestStepFinished& testStepFinished)
                    {
                        return totalDuration + testStepFinished.testStepResult.duration;
                    });

            return testRunHookFinishedDuration + testStepFinishedDuration;
        }

        void PrintNonPassingSteps(const messages::TestCaseFinished& testCaseFinished,
            [[maybe_unused]] messages::TestStepResultStatus ignoredStatus)
        {
            fmt::print(stream, "{}", stepFormatter.FormatNonPassingSteps(testCaseFinished));
        }

        void FormatHookLineTo(const messages::TestRunHookFinished& testRunHookFinished, LineBuilder& lineBuilder)
        {
            testRunHookFormatter.FormatHookLineTo(lineBuilder, testRunHookFinished);
        }

        void PrintTestRunHookException(const messages::TestRunHookFinished& testRunHookFinished,
            [[maybe_unused]] messages::TestStepResultStatus status)
        {
            fmt::print(stream, "{}", testRunHookFormatter.FormatException(testRunHookFinished));
        }

        void FormatLocationCommentTo(LineBuilder& lineBuilder, const messages::Pickle& pickle) const
        {
            const auto* location = data.FindLocationOf(pickle);
            AppendLocationComment(lineBuilder, sourceReferenceFormatter.Format(pickle.uri, location));
        }

        std::string FormatAttempt(const messages::TestCaseStarted& testCaseStarted) const
        {
            const auto attempt = testCaseStarted.attempt;
            if (attempt == 0)
            {
                return "";
            }
            return ", after " + std::to_string(attempt + 1) + " attempts";
        }

        template<class T, class U, class V>
        void PrintFinishedItemByStatus(std::string finishedItemName,
            std::map<messages::TestStepResultStatus, std::vector<T>> finishedItemByStatus, messages::TestStepResultStatus status,
            U&& formatFinishedItem, V&& printSupplementaryContent)
        {
            if (finishedItemByStatus.find(status) == finishedItemByStatus.end())
            {
                return;
            }

            fmt::println(stream, "{}",
                LineBuilder{ theme }
                    .NewLine()
                    .Append(theme->Style(Theme::Element::step, status,
                        fmt::format("{} {}:", SentenceCase(messages::to_string(status)), finishedItemName)))
                    .Build());

            const auto& finishedItems = finishedItemByStatus.at(status);
            for (auto index{ 0 }; index < finishedItems.size(); ++index)
            {
                const auto& finishedItem = finishedItems.at(index);
                fmt::println(stream, "{}",
                    LineBuilder{ theme }
                        .Append("  ")
                        .Append(std::to_string(index + 1))
                        .Append(") ")
                        .Accept(
                            [this, &formatFinishedItem, &finishedItem](LineBuilder& lineBuilder)
                            {
                                std::invoke(std::forward<U>(formatFinishedItem), this, finishedItem, lineBuilder);
                            })
                        .Build());

                std::invoke(std::forward<V>(printSupplementaryContent), this, finishedItem, status);
            }
        }

        std::ostream& stream; // NOLINT(cppcoreguidelines-avoid-const-or-ref-data-members) : ostream isn't copyable
        Data& data;
        std::shared_ptr<Theme> theme;

        std::function<std::string(std::string)> uriFormatter;

        std::set<enum Options> options;

        SourceReferenceFormatter sourceReferenceFormatter{ uriFormatter };

        constexpr static auto stepIndent{ 7 };
        StepFormatter stepFormatter{ data, theme, sourceReferenceFormatter, stepIndent,
            options.find(Options::includeAttachments) != options.end() };

        constexpr static auto hookIndent{ 7 };
        TestRunHookFormatter testRunHookFormatter{ data, theme, sourceReferenceFormatter, hookIndent };
    };

    SummaryPrinter::SummaryPrinter([[maybe_unused]] const ProtectedConstructorTag& tag, std::ostream& stream,
        std::shared_ptr<struct Theme> theme, std::function<std::string(std::string)> uriFormatter, std::set<enum Options> options)
        : data{ std::make_unique<Data>() }
        , printer{ std::make_unique<Printer>(stream, *data, theme, uriFormatter, std::move(options)) }
    {}

    void SummaryPrinter::Update(const messages::Envelope& envelope)
    {
        data->Update(envelope);

        if (envelope.testRunFinished.has_value())
        {
            printer->PrintSummary();
        }
    }

    /////////////////////////////////////////////////////////////////////

    SummaryPrinter::Factory::Factory()
        : theme{ Theme::None() }
        , uriFormatter{ [](std::string uri)
            {
                return uri;
            } }
        , options{ Options::includeAttachments }
    {}

    SummaryPrinter::Factory& SummaryPrinter::Factory::Theme(std::shared_ptr<struct Theme> theme)
    {
        this->theme = std::move(theme);
        return *this;
    }

    SummaryPrinter::Factory& SummaryPrinter::Factory::RemoveUriPrefix(std::string prefix)
    {
        uriFormatter = [prefix = std::move(prefix)](std::string uri)
        {
            if (uri.rfind(prefix, 0) == 0)
            {
                return uri.substr(prefix.size());
            }
            return uri;
        };
        return *this;
    }

    SummaryPrinter::Factory& SummaryPrinter::Factory::Options(enum Options option, bool enabled)
    {
        if (enabled)
        {
            options.insert(option);
        }
        else
        {
            options.erase(option);
        }
        return *this;
    }

    std::unique_ptr<Formatter> SummaryPrinter::Factory::Build(std::ostream& stream) const
    {
        return std::make_unique<SummaryPrinter>(SummaryPrinter::ProtectedConstructorTag{}, stream, theme, uriFormatter, options);
    }
}
