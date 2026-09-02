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
#include "cucumber/pretty-formatter/AmbiguousStepDefinitionsFormatter.hpp"
#include "cucumber/pretty-formatter/AttachmentFormatter.hpp"
#include "cucumber/pretty-formatter/ExceptionFormatter.hpp"
#include "cucumber/pretty-formatter/Formatter.hpp"
#include "cucumber/pretty-formatter/LineBuilder.hpp"
#include "cucumber/pretty-formatter/Theme.hpp"
#include "cucumber/query/Query.hpp"
#include <algorithm>
#include <cctype>
#include <chrono>
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
        const std::set allStatuses = {
            messages::TestStepResultStatus::UNKNOWN,
            messages::TestStepResultStatus::PASSED,
            messages::TestStepResultStatus::SKIPPED,
            messages::TestStepResultStatus::PENDING,
            messages::TestStepResultStatus::UNDEFINED,
            messages::TestStepResultStatus::AMBIGUOUS,
            messages::TestStepResultStatus::FAILED,
        };

        const std::set nonFailedStatuses = {
            messages::TestStepResultStatus::PASSED,
            messages::TestStepResultStatus::SKIPPED,
        };

        const std::set failingStatuses = []()
        {
            std::set<messages::TestStepResultStatus> result;
            std::set_difference(allStatuses.begin(), allStatuses.end(), nonFailedStatuses.begin(), nonFailedStatuses.end(),
                std::inserter(result, result.end()));
            return result;
        }();

        std::int32_t PickleComparator(const std::shared_ptr<const messages::Pickle>& lhs,
            const std::shared_ptr<const messages::Pickle>& rhs)

        {
            if (lhs->uri != rhs->uri)
            {
                return static_cast<std::int32_t>(rhs->uri.compare(lhs->uri));
            }
            if (!lhs->location.has_value() || !rhs->location.has_value())
            {
                return 0;
            }
            if (lhs->location.value()->line != rhs->location.value()->line)
            {
                return static_cast<std::int32_t>(lhs->location.value()->line) - static_cast<std::int32_t>(rhs->location.value()->line);
            }
            return static_cast<std::int32_t>(lhs->location.value()->column.value_or(0)) -
                   static_cast<std::int32_t>(rhs->location.value()->column.value_or(0));
        }

        char ToLower(char chr)
        {
            return static_cast<char>(std::tolower(chr));
        }

        std::string ToLower(std::string text)
        {
            std::transform(text.begin(), text.end(), text.begin(), static_cast<char (*)(char)>(ToLower));
            return text;
        }

        std::string ToLower(std::string_view text)
        {
            return ToLower(std::string{ text });
        }

        char ToUpper(char chr)
        {
            return static_cast<char>(std::toupper(chr));
        }

        std::string ToUpper(std::string text)
        {
            std::transform(text.begin(), text.end(), text.begin(), static_cast<char (*)(char)>(ToUpper));
            return text;
        }

        std::string ToUpper(std::string_view text)
        {
            return ToUpper(std::string{ text });
        }

        std::string SentenceCase(std::string text)
        {
            if (text.empty())
            {
                return text;
            }

            std::transform(text.begin(), text.end(), text.begin(), static_cast<char (*)(char)>(ToLower));

            text.front() = ToUpper(text.front());

            return text;
        }

        std::string SentenceCase(std::string_view text)
        {
            return SentenceCase(std::string{ text });
        }

        template<class T, class StatusOf>
        std::string FormatSubCounts(std::string_view singular, std::string_view plural,
            const std::vector<std::shared_ptr<const T>>& finishedItems, const Theme& theme, StatusOf&& statusOf)
        {
            const auto size = finishedItems.size();
            const auto countAndName = fmt::format("{} {}", size, size == 1 ? singular : plural);

            std::map<messages::TestStepResultStatus, std::size_t> counts;
            for (const auto& item : finishedItems)
            {
                ++counts[std::invoke(std::forward<StatusOf>(statusOf), item)];
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

        std::chrono::minutes ToMinutes(const std::shared_ptr<const messages::Duration>& duration)
        {
            return std::chrono::minutes{ duration->seconds / 60 };
        }

        std::chrono::seconds ToSeconds(const std::shared_ptr<const messages::Duration>& duration)
        {
            return std::chrono::seconds{ duration->seconds % 60 };
        }

        std::chrono::milliseconds ToMilliSeconds(const std::shared_ptr<const messages::Duration>& duration)
        {
            return std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::nanoseconds{ duration->nanos });
        }

        std::string FormatDuration(const std::shared_ptr<const messages::Duration>& duration)
        {
            return fmt::format("{}m {}.{:03}s", ToMinutes(duration).count(), ToSeconds(duration).count(), ToMilliSeconds(duration).count());
        }

        template<typename Proj, typename Instance, typename ListType>
        auto GroupBy(Instance&& instance, Proj&& proj, const std::vector<std::shared_ptr<const ListType>>& list)
        {
            std::map<messages::TestStepResultStatus, std::vector<std::shared_ptr<const ListType>>> groupedByProj;

            for (const auto& item : list)
            {
                groupedByProj[std::invoke(std::forward<Proj>(proj), std::forward<Instance>(instance), item)].push_back(item);
            }

            return groupedByProj;
        }

        const std::unordered_map<messages::HookType, std::string_view> formatHookType = {
            { messages::HookType::BEFORE_TEST_RUN, "BeforeAll" },
            { messages::HookType::AFTER_TEST_RUN, "AfterAll" },
            { messages::HookType::BEFORE_TEST_CASE, "Before" },
            { messages::HookType::AFTER_TEST_CASE, "After" },
            { messages::HookType::BEFORE_TEST_STEP, "BeforeStep" },
            { messages::HookType::AFTER_TEST_STEP, "AfterStep" },
        };
    }

    SummaryPrinter::SummaryPrinter([[maybe_unused]] const ProtectedConstructorTag& tag, std::ostream& stream,
        std::shared_ptr<struct Theme> theme, std::function<std::string(std::string)> uriFormatter, std::set<enum Options> options)
        : stream{ stream }
        , theme{ std::move(theme) }
        , uriFormatter{ std::move(uriFormatter) }
        , options{ std::move(options) }
    {}

    void SummaryPrinter::Update(const messages::Envelope& envelope)
    {
        query.Update(envelope);

        if (envelope.testRunFinished.has_value())
        {
            PrintSummary();
        }
    }

    void SummaryPrinter::PrintSummary()
    {
        PrintNonPassingScenarios();
        PrintUnknownParameterTypes();
        PrintNonPassingGlobalHooks();
        PrintNonPassingTestRun();
        PrintStats();
        PrintSnippets();
    }

    void SummaryPrinter::PrintNonPassingScenarios()
    {
        const auto& allTestCasesFinished = query.FindAllTestCaseFinishedOrderBy(query::findPickleByTestCaseFinished, PickleComparator);
        const auto& testCaseFinishedByStatus =
            GroupBy(this, &SummaryPrinter::GetTestStepResultStatusByTestCaseFinished, allTestCasesFinished);

        for (const auto& status : failingStatuses)
        {
            PrintFinishedItemByStatus("scenarios", testCaseFinishedByStatus, status, &SummaryPrinter::FormatScenarioLineTo,
                &SummaryPrinter::PrintNonPassingSteps);
        }
    }

    void SummaryPrinter::PrintUnknownParameterTypes()
    {
        const auto& undefinedParameterTypes = query.FindAllUndefinedParameterTypes();
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
            fmt::println(stream, "  {}) '{}' in '{}'", ++index, undefinedParameterType->name, undefinedParameterType->expression);
        }
    }

    void SummaryPrinter::PrintNonPassingGlobalHooks()
    {
        const auto& allTestRunHooksFinished = query.FindAllTestRunHookFinished();
        const auto& testRunHookFinishedByStatus =
            GroupBy(this, &SummaryPrinter::GetTestStepResultStatusByTestRunHookFinished, allTestRunHooksFinished);

        for (const auto& status : failingStatuses)
        {
            PrintFinishedItemByStatus("hooks", testRunHookFinishedByStatus, status, &SummaryPrinter::FormatHookLineTo,
                &SummaryPrinter::PrintTestRunHookException);
        }
    }

    void SummaryPrinter::PrintNonPassingTestRun()
    {
        const auto& optException = GetTestRunWithException();
        if (optException.has_value())
        {
            fmt::println(stream, "{}",
                theme->Style(Theme::Element::step, messages::TestStepResultStatus::FAILED,
                    SentenceCase(messages::to_string(messages::TestStepResultStatus::FAILED)) + " test run:"));

            constexpr auto indent{ 7 };
            ExceptionFormatter exceptionFormatter{ indent, theme, messages::TestStepResultStatus::FAILED };

            const auto formattedException = exceptionFormatter.Format(optException.value());
            if (formattedException.has_value())
            {
                fmt::print(stream, "{}", formattedException.value());
            }
        }
    }

    void SummaryPrinter::PrintStats()
    {
        fmt::println(stream, "");
        PrintTestRunCount();
        PrintGlobalHookCount();
        PrintScenarioCounts();
        PrintStepCounts();
        PrintDurations();
    }

    void SummaryPrinter::PrintTestRunCount()
    {
        const auto& optException = GetTestRunWithException();
        if (optException.has_value())
        {
            fmt::println(stream, "1 test run ({})", theme->Style(Theme::Element::step, messages::TestStepResultStatus::FAILED, "1 failed"));
        }
    }

    void SummaryPrinter::PrintGlobalHookCount()
    {
        const auto& allTestRunHookFinished = query.FindAllTestRunHookFinished();
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

    void SummaryPrinter::PrintScenarioCounts()
    {
        fmt::println(stream, "{}",
            FormatSubCounts("scenario", "scenarios", query.FindAllTestCaseFinished(), *theme,
                [this](const auto& item)
                {
                    return GetTestStepResultStatusByTestCaseFinished(item);
                }));
    }

    void SummaryPrinter::PrintStepCounts()
    {
        const auto& allTestCasesFinished = query.FindAllTestCaseFinished();
        std::vector<std::shared_ptr<const messages::TestStepFinished>> testStepsFinished;
        for (const auto& testCaseFinished : allTestCasesFinished)
        {
            const auto& testStepsFinishedForCase = query.FindTestStepsFinishedBy(testCaseFinished);
            testStepsFinished.insert(testStepsFinished.end(), testStepsFinishedForCase.begin(), testStepsFinishedForCase.end());
        }

        fmt::println(stream, "{}",
            FormatSubCounts("step", "steps", testStepsFinished, *theme,
                [this](const auto& item)
                {
                    return item->testStepResult->status;
                }));
    }

    void SummaryPrinter::PrintDurations()
    {
        const auto& optRunDuration = query.FindTestRunDuration();
        if (optRunDuration.has_value())
        {
            fmt::println(stream, "{} ({} executing your code)", FormatDuration(*optRunDuration), FormatDuration(GetExecutionDuration()));
        }
    }

    void SummaryPrinter::PrintSnippets()
    {
        const auto& allTestCasesFinishedOrdered =
            query.FindAllTestCaseFinishedOrderBy(query::findPickleByTestCaseFinished, PickleComparator);

        std::vector<std::shared_ptr<const messages::Snippet>> snippets;
        std::unordered_set<std::string> seen;

        for (const auto& testCaseFinished : allTestCasesFinishedOrdered)
        {
            const auto& optPickle = query.FindPickleBy(testCaseFinished);
            if (optPickle.has_value())
            {
                const auto& pickle = optPickle.value();
                const auto& suggestions = query.FindSuggestionsBy(pickle);

                for (const auto& suggestion : suggestions)
                {
                    for (const auto& snippet : suggestion->snippets)
                    {
                        if (seen.insert(snippet->language + "-" + snippet->code).second)
                        {
                            snippets.push_back(snippet);
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

    void SummaryPrinter::PrintStep(const std::shared_ptr<const messages::TestStepFinished>& testStepFinished,
        const std::shared_ptr<const messages::TestStep>& testStep)
    {
        const auto status = testStepFinished->testStepResult->status;
        const auto& optPickleStep = query.FindPickleStepBy(testStep);

        if (optPickleStep.has_value())
        {
            const auto& pickleStep = optPickleStep.value();
            const auto& optStep = query.FindStepBy(pickleStep);
            if (optStep.has_value())
            {
                const auto& step = optStep.value();
                fmt::println(stream, "{}", FormatPickleStep(testStepFinished, testStep, pickleStep, step));

                if (pickleStep->argument.has_value())
                {
                    if (pickleStep->argument.value()->dataTable.has_value())
                    {
                        fmt::print(stream, "{}",
                            LineBuilder{ theme }
                                .Accept(
                                    [this, &testStepFinished, &pickleStep](LineBuilder& lineBuilder)
                                    {
                                        pickleTableFormatter.Format(lineBuilder, pickleStep->argument.value()->dataTable.value());
                                    })
                                .Build());
                    }

                    if (pickleStep->argument.value()->docString.has_value())
                    {
                        fmt::print(stream, "{}",
                            LineBuilder{ theme }
                                .Accept(
                                    [this, &testStepFinished, &pickleStep](LineBuilder& lineBuilder)
                                    {
                                        pickleDocStringFormatter.Format(lineBuilder, pickleStep->argument.value()->docString.value());
                                    })
                                .Build());
                    }
                }

                if (status == messages::TestStepResultStatus::AMBIGUOUS)
                {
                    fmt::print(stream, "{}",
                        LineBuilder{ theme }
                            .Accept(
                                [this, &testStep](LineBuilder& lineBuilder)
                                {
                                    constexpr auto indent = 11;
                                    AmbiguousStepDefinitionsFormatter{ indent, theme, sourceReferenceFormatter }.Format(lineBuilder,
                                        query.FindStepDefinitionsBy(testStep));
                                })
                            .Build());
                }
            }
        }

        const auto& optHook = query.FindHookBy(testStep);
        if (optHook.has_value())
        {
            const auto& hook = optHook.value();
            fmt::println(stream, "{}", FormatHookStep(testStepFinished, hook));
        }

        const auto testStepResult = testStepFinished->testStepResult;
        constexpr auto indent = 11;
        ExceptionFormatter exceptionFormatter{ indent, theme, status };
        const auto& message = testStepResult->message;

        if (testStepResult->exception.has_value())
        {
            fmt::print(stream, "{}", exceptionFormatter.Format(testStepResult->exception.value(), message).value_or(""));
        }
        else if (message.has_value())
        {
            fmt::print(stream, "{}", exceptionFormatter.Format(message.value()));
        }

        if (options.find(SummaryPrinter::Options::includeAttachments) != options.end())
        {
            const auto& attachments = query.FindAttachmentsBy(testStepFinished);
            for (const auto& attachment : attachments)
            {
                fmt::print(stream, "{}",
                    LineBuilder{ theme }
                        .NewLine()
                        .Accept(
                            [this, &attachment](LineBuilder& lineBuilder)
                            {
                                constexpr auto indent = 11;
                                AttachmentFormatter{ indent }.Format(lineBuilder, attachment);
                            })
                        .Build());
            }
        }
    }

    /////////////////////////////////////////////////////////////////////

    messages::TestStepResultStatus SummaryPrinter::GetTestStepResultStatusByTestCaseFinished(
        const std::shared_ptr<const messages::TestCaseFinished>& testCaseFinished) const
    {
        const auto mostSevereTestStepResult = query.FindMostSevereTestStepResultBy(testCaseFinished);
        if (mostSevereTestStepResult.has_value())
        {
            return mostSevereTestStepResult.value()->status;
        }
        return messages::TestStepResultStatus::PASSED;
    }

    messages::TestStepResultStatus SummaryPrinter::GetTestStepResultStatusByTestRunHookFinished(
        const std::shared_ptr<const messages::TestRunHookFinished>& testRunHookFinished) const
    {
        return testRunHookFinished->result->status;
    }

    messages::TestStepResultStatus SummaryPrinter::GetTestStepResultStatusByTestStepFinished(
        const std::shared_ptr<const messages::TestStepFinished>& testStepFinished) const
    {
        return testStepFinished->testStepResult->status;
    }

    std::optional<std::shared_ptr<const messages::Exception>> SummaryPrinter::GetTestRunWithException() const
    {
        const auto& optTestRunFinished = query.FindTestRunFinished();

        if (optTestRunFinished.has_value())
        {
            const auto& testRunFinished = optTestRunFinished.value();
            if (!testRunFinished->success)
            {
                return testRunFinished->exception;
            }
        }

        return std::nullopt;
    }

    std::shared_ptr<const messages::Duration> SummaryPrinter::GetExecutionDuration() const
    {
        const auto& allTestRunHookFinished = query.FindAllTestRunHookFinished();
        const auto& allTestStepFinished = query.FindAllTestStepFinished();

        const auto testRunHookFinishedDuration = std::accumulate(allTestRunHookFinished.begin(), allTestRunHookFinished.end(),
            messages::Duration{},
            [this](const messages::Duration& totalDuration, const std::shared_ptr<const messages::TestRunHookFinished>& testRunHookFinished)
            {
                return totalDuration + *testRunHookFinished->result->duration;
            });

        const auto testStepFinishedDuration = std::accumulate(allTestStepFinished.begin(), allTestStepFinished.end(), messages::Duration{},
            [this](const messages::Duration& totalDuration, const std::shared_ptr<const messages::TestStepFinished>& testStepFinished)
            {
                return totalDuration + *testStepFinished->testStepResult->duration;
            });

        return std::make_shared<messages::Duration>(testRunHookFinishedDuration + testStepFinishedDuration);
    }

    void SummaryPrinter::FormatScenarioLineTo(const std::shared_ptr<const messages::TestCaseFinished>& testCaseFinished,
        LineBuilder& lineBuilder)
    {
        const auto optTestCaseStarted = query.FindTestCaseStartedBy(testCaseFinished);

        if (!optTestCaseStarted.has_value())
        {
            return;
        }

        const auto& testCaseStarted = optTestCaseStarted.value();
        const auto optPickle = query.FindPickleBy(testCaseStarted);

        if (optPickle.has_value())
        {
            const auto& pickle = optPickle.value();
            lineBuilder.Append(pickle->name)
                .Append(FormatAttempt(testCaseStarted))
                .Accept(
                    [this, &pickle](LineBuilder& lineBuilder)
                    {
                        FormatLocationCommentTo(lineBuilder, pickle);
                    });
        }
    }

    namespace
    {
        //move
        std::vector<query::TestStepFinishedAndTestStep> FindNonPassingSteps(
            const std::vector<query::TestStepFinishedAndTestStep>& allTestStepFinishedAndTestStep)
        {
            std::vector<query::TestStepFinishedAndTestStep> nonPassingSteps;
            bool foundFirstNonPassed = false;

            for (const auto& [testStepFinished, testStep] : allTestStepFinishedAndTestStep)
            {
                const auto status = testStepFinished->testStepResult->status;
                if (foundFirstNonPassed)
                {
                    if (status != messages::TestStepResultStatus::PASSED && status != messages::TestStepResultStatus::SKIPPED)
                    {
                        nonPassingSteps.push_back({ testStepFinished, testStep });
                    }
                }
                else if (status != messages::TestStepResultStatus::PASSED)
                {
                    nonPassingSteps.push_back({ testStepFinished, testStep });
                    foundFirstNonPassed = true;
                }
            }

            return nonPassingSteps;
        }
    }

    void SummaryPrinter::PrintNonPassingSteps(const std::shared_ptr<const messages::TestCaseFinished>& testCaseFinished,
        [[maybe_unused]] messages::TestStepResultStatus ignoredStatus)
    {
        const auto optTestCaseStarted = query.FindTestCaseStartedBy(testCaseFinished);
        if (!optTestCaseStarted.has_value())
        {
            return;
        }

        const auto allTestStepFinishedAndTestStep = query.FindTestStepFinishedAndTestStepBy(optTestCaseStarted.value());
        if (allTestStepFinishedAndTestStep.empty())
        {
            return;
        }

        const auto nonPassingSteps = FindNonPassingSteps(allTestStepFinishedAndTestStep);
        for (const auto& [testStepFinished, testStep] : nonPassingSteps)
        {
            PrintStep(testStepFinished, testStep);
        }
    }

    void SummaryPrinter::FormatHookLineTo(const std::shared_ptr<const messages::TestRunHookFinished>& testRunHookFinished,
        LineBuilder& lineBuilder)
    {
        const auto& optHook = query.FindHookBy(testRunHookFinished);
        if (optHook.has_value())
        {
            const auto& hook = optHook.value();
            lineBuilder.Append(hook->type.has_value() ? formatHookType.at(hook->type.value()) : "Unknown")
                .Accept(
                    [this, &hook](auto& lineBuilder)
                    {
                        if (hook->name.has_value())
                        {
                            const auto& name = hook->name.value();
                            lineBuilder.Append(fmt::format("({})", name));
                        }
                    })
                .Accept(
                    [this, &hook](auto& lineBuilder)
                    {
                        FormatLocationCommentTo(lineBuilder, hook);
                    });
        }
    }

    void SummaryPrinter::PrintTestRunHookException(const std::shared_ptr<const messages::TestRunHookFinished>& testRunHookFinished,
        [[maybe_unused]] messages::TestStepResultStatus status)
    {
        const auto& result = testRunHookFinished->result;
        constexpr auto indent = 7;
        ExceptionFormatter exceptionFormatter{ indent, theme, status };
        const auto& message = result->message;

        if (result->exception.has_value())
        {
            fmt::print(stream, "{}", exceptionFormatter.Format(result->exception.value(), message).value_or(""));
        }
        else if (message.has_value())
        {
            fmt::print(stream, "{}", exceptionFormatter.Format(message.value()));
        }
    }

    void SummaryPrinter::FormatLocationCommentTo(LineBuilder& lineBuilder, const std::shared_ptr<const messages::Pickle>& pickle) const
    {
        const auto& comment = sourceReferenceFormatter.Format(pickle->uri, query.FindLocationOf(pickle));
        FormatLocationCommentTo(lineBuilder, comment);
    }

    void SummaryPrinter::FormatLocationCommentTo(LineBuilder& lineBuilder, const std::shared_ptr<const messages::TestStep>& testStep) const
    {
        const auto& optUnambiguousStepDefinition = query.FindUnambiguousStepDefinitionBy(testStep);

        if (!optUnambiguousStepDefinition.has_value())
        {
            return;
        }

        const auto& sourceReference = optUnambiguousStepDefinition.value()->sourceReference;
        const auto& comment = sourceReferenceFormatter.Format(sourceReference);
        FormatLocationCommentTo(lineBuilder, comment);
    }

    void SummaryPrinter::FormatLocationCommentTo(LineBuilder& lineBuilder, const std::shared_ptr<const messages::Hook>& hook) const
    {
        const auto& comment = sourceReferenceFormatter.Format(hook->sourceReference);
        FormatLocationCommentTo(lineBuilder, comment);
    }

    void SummaryPrinter::FormatLocationCommentTo(LineBuilder& lineBuilder, const std::optional<std::string>& comment) const
    {
        if (comment.has_value())
        {
            FormatLocationCommentTo(lineBuilder, comment.value());
        }
    }

    void SummaryPrinter::FormatLocationCommentTo(LineBuilder& lineBuilder, const std::string& comment) const
    {
        lineBuilder.Append(" ").Append(Theme::Element::location, "# " + comment);
    }

    std::string SummaryPrinter::FormatAttempt(const std::shared_ptr<const messages::TestCaseStarted>& testCaseStarted) const
    {
        const auto attempt = testCaseStarted->attempt;
        if (attempt == 0)
        {
            return "";
        }
        return ", after " + std::to_string(attempt + 1) + " attempts";
    }

    std::string SummaryPrinter::FormatHookStep(const std::shared_ptr<const messages::TestStepFinished>& testStepFinished,
        const std::shared_ptr<const messages::Hook>& hook) const
    {
        const auto& status = testStepFinished->testStepResult->status;
        constexpr auto indent{ 7 };
        return LineBuilder{ theme }
            .Indent(indent)
            .Begin(Theme::Element::step, status)
            .Append(Theme::Element::stepKeyword, hook->type.has_value() ? formatHookType.at(hook->type.value()) : "Unknown")
            .Append(hook->name.has_value() ? "(" + hook->name.value() + ")" : "")
            .End(Theme::Element::step, status)
            .Accept(
                [this, &hook](auto& lineBuilder)
                {
                    FormatLocationCommentTo(lineBuilder, hook);
                })
            .Build();
    }

    std::string SummaryPrinter::FormatPickleStep(const std::shared_ptr<const messages::TestStepFinished>& testStepFinished,
        const std::shared_ptr<const messages::TestStep>& testStep, const std::shared_ptr<const messages::PickleStep>& pickleStep,
        const std::shared_ptr<const messages::Step>& step) const
    {
        const auto status = testStepFinished->testStepResult->status;
        constexpr auto indent{ 7 };
        return LineBuilder{ theme }
            .Indent(indent)
            .Begin(Theme::Element::step, status)
            .Append(Theme::Element::stepKeyword, step->keyword)
            .Accept(
                [this, &testStep, &pickleStep](auto& lineBuilder)
                {
                    stepTextFormatter.Format(lineBuilder, testStep, pickleStep);
                })
            .End(Theme::Element::step, status)
            .Accept(
                [this, &testStep](auto& lineBuilder)
                {
                    FormatLocationCommentTo(lineBuilder, testStep);
                })
            .Build();
    }

    template<class T, class U, class V>
    void SummaryPrinter::PrintFinishedItemByStatus(std::string finishedItemname,
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
                    fmt::format("{} {}:", SentenceCase(messages::to_string(status)), finishedItemname)))
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
