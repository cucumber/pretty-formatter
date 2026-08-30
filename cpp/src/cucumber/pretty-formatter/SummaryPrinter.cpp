#include "cucumber/pretty-formatter/SummaryPrinter.hpp"
#include "cucumber/messages/Envelope.hpp"
#include "cucumber/messages/Hook.hpp"
#include "cucumber/messages/Pickle.hpp"
#include "cucumber/messages/PickleStep.hpp"
#include "cucumber/messages/Step.hpp"
#include "cucumber/messages/TestCaseFinished.hpp"
#include "cucumber/messages/TestCaseStarted.hpp"
#include "cucumber/messages/TestStep.hpp"
#include "cucumber/messages/TestStepFinished.hpp"
#include "cucumber/messages/TestStepResultStatus.hpp"
#include "cucumber/pretty-formatter/AmbiguousStepDefinitionsFormatter.hpp"
#include "cucumber/pretty-formatter/ExceptionFormatter.hpp"
#include "cucumber/pretty-formatter/Formatter.hpp"
#include "cucumber/pretty-formatter/LineBuilder.hpp"
#include "cucumber/pretty-formatter/Theme.hpp"
#include "cucumber/query/Query.hpp"
#include <algorithm>
#include <cstdint>
#include <fmt/ostream.h>
#include <functional>
#include <iterator>
#include <map>
#include <memory>
#include <optional>
#include <ostream>
#include <set>
#include <string>
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
    }

    SummaryPrinter::SummaryPrinter(const ProtectedConstructorTag&, std::ostream& stream, std::shared_ptr<struct Theme> theme,
        std::function<std::string(std::string)> uriFormatter, std::set<enum Options> options)
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

    namespace
    {
        auto PickleComparator(const std::shared_ptr<const messages::Pickle>& lhs, const std::shared_ptr<const messages::Pickle>& rhs)
            -> std::int32_t
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
    }

    void SummaryPrinter::PrintNonPassingScenarios()
    {
        const auto allTestCasesFinished = query.FindAllTestCaseFinishedOrderBy(query::findPickleByTestCaseFinished, PickleComparator);
        std::map<messages::TestStepResultStatus, std::vector<std::shared_ptr<const messages::TestCaseFinished>>> testCaseFinishedByStatus;
        for (const auto& testCaseFinished : allTestCasesFinished)
        {
            testCaseFinishedByStatus[GetTestStepResultStatusBy(testCaseFinished)].push_back(testCaseFinished);
        }

        for (const auto& status : failingStatuses)
        {
            PrintFinishedItemByStatus("scenarios", testCaseFinishedByStatus, status, &SummaryPrinter::FormatScenarioLineTo,
                &SummaryPrinter::PrintNonPassingSteps);
        }
    }

    void SummaryPrinter::PrintUnknownParameterTypes()
    {}

    void SummaryPrinter::PrintNonPassingGlobalHooks()
    {}

    void SummaryPrinter::PrintNonPassingTestRun()
    {}

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
    {}

    void SummaryPrinter::PrintGlobalHookCount()
    {}

    void SummaryPrinter::PrintScenarioCounts()
    {}

    void SummaryPrinter::PrintStepCounts()
    {}

    void SummaryPrinter::PrintDurations()
    {}

    void SummaryPrinter::PrintSnippets()
    {}

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

        // TODO        query.findHookBy(testStep).ifPresent(hook->{ out.println(formatHookStep(testStepFinished, hook)); });

        const auto testStepResult = testStepFinished->testStepResult;
        constexpr auto indent = 11;
        ExceptionFormatter exceptionFormatter{ indent, theme, status };
        const auto message = testStepResult->message;

        if (testStepResult->exception.has_value())
        {
            fmt::print(stream, "{}", exceptionFormatter.Format(testStepResult->exception.value(), message).value_or(""));
        }
        else if (message.has_value())
        {
            fmt::print(stream, "{}", exceptionFormatter.Format(message.value()));
        }

        //         if (features.contains(MessagesToSummaryWriter.SummaryFeature.INCLUDE_ATTACHMENTS))
        //         {
        //             query.findAttachmentsBy(testStepFinished)
        //                 .forEach(attachment->out.print(new LineBuilder(theme)
        //                         .newLine()
        //                         .accept(lineBuilder->AttachmentFormatter.builder().indentation(11).build().formatTo(attachment, lineBuilder))
        //                         .build()));
        //         }
    }

    /////////////////////////////////////////////////////////////////////

    messages::TestStepResultStatus SummaryPrinter::GetTestStepResultStatusBy(
        const std::shared_ptr<const messages::TestCaseFinished>& testCaseFinished)
    {
        const auto mostSevereTestStepResult = query.FindMostSevereTestStepResultBy(testCaseFinished);
        if (mostSevereTestStepResult.has_value())
        {
            return mostSevereTestStepResult.value()->status;
        }
        return messages::TestStepResultStatus::PASSED;
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

    std::string SummaryPrinter::FormatPickleStep(const std::shared_ptr<const messages::TestStepFinished>& testStepFinished,
        const std::shared_ptr<const messages::TestStep>& testStep, const std::shared_ptr<const messages::PickleStep>& pickleStep,
        const std::shared_ptr<const messages::Step>& step)
    {
        const auto status = testStepFinished->testStepResult->status;
        return LineBuilder{ theme }
            .Indent(7)
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

    /////////////////////////////////////////////////////////////////////

    SummaryPrinter::Factory::Factory()
        : theme{ Theme::None() }
        , uriFormatter{ [](std::string uri)
            {
                return uri;
            } }
        , options{ Options::includeFeatureLine, Options::includeRuleLine, Options::useStatusIcon, Options::includeAttachments }
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
        auto optionsCopy = options;
        if (!theme->HasStatusIcons())
        {
            optionsCopy.erase(Options::useStatusIcon);
        }

        return std::make_unique<SummaryPrinter>(SummaryPrinter::ProtectedConstructorTag{}, stream, theme, uriFormatter, optionsCopy);
    }
}
