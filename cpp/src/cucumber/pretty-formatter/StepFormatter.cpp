#include "cucumber/pretty-formatter/StepFormatter.hpp"
#include "cucumber/messages/Hook.hpp"
#include "cucumber/messages/HookType.hpp"
#include "cucumber/messages/PickleStep.hpp"
#include "cucumber/messages/Step.hpp"
#include "cucumber/messages/TestCaseFinished.hpp"
#include "cucumber/messages/TestStep.hpp"
#include "cucumber/messages/TestStepFinished.hpp"
#include "cucumber/messages/TestStepResultStatus.hpp"
#include "cucumber/pretty-formatter/AmbiguousStepDefinitionsFormatter.hpp"
#include "cucumber/pretty-formatter/AttachmentFormatter.hpp"
#include "cucumber/pretty-formatter/ExceptionFormatter.hpp"
#include "cucumber/pretty-formatter/LineBuilder.hpp"
#include "cucumber/pretty-formatter/SourceReferenceFormatter.hpp"
#include "cucumber/pretty-formatter/Theme.hpp"
#include "cucumber/query/Query.hpp"
#include <cstddef>
#include <fmt/core.h>
#include <fmt/format.h>
#include <fmt/ostream.h>
#include <memory>
#include <optional>
#include <sstream>
#include <string>
#include <string_view>
#include <unordered_map>
#include <utility>
#include <vector>

namespace cucumber::pretty_formatter
{
    namespace
    {
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

        const std::unordered_map<messages::HookType, std::string_view> formatHookType = {
            { messages::HookType::BEFORE_TEST_RUN, "BeforeAll" },
            { messages::HookType::AFTER_TEST_RUN, "AfterAll" },
            { messages::HookType::BEFORE_TEST_CASE, "Before" },
            { messages::HookType::AFTER_TEST_CASE, "After" },
            { messages::HookType::BEFORE_TEST_STEP, "BeforeStep" },
            { messages::HookType::AFTER_TEST_STEP, "AfterStep" },
        };

        void FormatLocationCommentTo(LineBuilder& lineBuilder, const std::string& comment)
        {
            lineBuilder.Append(" ").Append(Theme::Element::location, "# " + comment);
        }

        void FormatLocationCommentTo(LineBuilder& lineBuilder, const std::optional<std::string>& comment)
        {
            if (comment.has_value())
            {
                FormatLocationCommentTo(lineBuilder, comment.value());
            }
        }
    }

    StepFormatter::StepFormatter(query::Query& data, std::shared_ptr<Theme> theme, SourceReferenceFormatter sourceReferenceFormatter,
        std::size_t indent, bool includeAttachments)
        : data{ data }
        , theme{ std::move(theme) }
        , sourceReferenceFormatter{ std::move(sourceReferenceFormatter) }
        , indent{ indent }
        , argumentIndent{ indent + 2 }
        , deepIndent{ indent + 4 }
        , includeAttachments{ includeAttachments }
    {}

    std::string StepFormatter::FormatNonPassingSteps(const std::shared_ptr<const messages::TestCaseFinished>& testCaseFinished)
    {
        const auto optTestCaseStarted = data.FindTestCaseStartedBy(testCaseFinished);
        if (!optTestCaseStarted.has_value())
        {
            return "";
        }

        const auto allTestStepFinishedAndTestStep = data.FindTestStepFinishedAndTestStepBy(optTestCaseStarted.value());
        if (allTestStepFinishedAndTestStep.empty())
        {
            return "";
        }

        std::string result;
        for (const auto& [testStepFinished, testStep] : FindNonPassingSteps(allTestStepFinishedAndTestStep))
        {
            result += FormatStep(testStepFinished, testStep);
        }

        return result;
    }

    std::string StepFormatter::FormatStep(const std::shared_ptr<const messages::TestStepFinished>& testStepFinished,
        const std::shared_ptr<const messages::TestStep>& testStep)
    {
        std::ostringstream stream;

        const auto status = testStepFinished->testStepResult->status;
        const auto& optPickleStep = data.FindPickleStepBy(testStep);

        if (optPickleStep.has_value())
        {
            const auto& pickleStep = optPickleStep.value();
            const auto& optStep = data.FindStepBy(pickleStep);
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
                                    [this, &pickleStep](LineBuilder& lineBuilder)
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
                                    [this, &pickleStep](LineBuilder& lineBuilder)
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
                                    AmbiguousStepDefinitionsFormatter{ deepIndent, theme, sourceReferenceFormatter }.Format(lineBuilder,
                                        data.FindStepDefinitionsBy(testStep));
                                })
                            .Build());
                }
            }
        }

        const auto& optHook = data.FindHookBy(testStep);
        if (optHook.has_value())
        {
            const auto& hook = optHook.value();
            fmt::println(stream, "{}", FormatHookStep(testStepFinished, hook));
        }

        const auto testStepResult = testStepFinished->testStepResult;
        ExceptionFormatter exceptionFormatter{ deepIndent, theme, status };
        const auto& message = testStepResult->message;

        if (testStepResult->exception.has_value())
        {
            fmt::print(stream, "{}", exceptionFormatter.Format(testStepResult->exception.value(), message).value_or(""));
        }
        else if (message.has_value())
        {
            fmt::print(stream, "{}", exceptionFormatter.Format(message.value()));
        }

        if (includeAttachments)
        {
            const auto& attachments = data.FindAttachmentsBy(testStepFinished);
            for (const auto& attachment : attachments)
            {
                fmt::print(stream, "{}",
                    LineBuilder{ theme }
                        .NewLine()
                        .Accept(
                            [this, &attachment](LineBuilder& lineBuilder)
                            {
                                AttachmentFormatter{ deepIndent }.Format(lineBuilder, attachment);
                            })
                        .Build());
            }
        }

        return stream.str();
    }

    std::string StepFormatter::FormatHookStep(const std::shared_ptr<const messages::TestStepFinished>& testStepFinished,
        const std::shared_ptr<const messages::Hook>& hook) const
    {
        const auto& status = testStepFinished->testStepResult->status;
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

    std::string StepFormatter::FormatPickleStep(const std::shared_ptr<const messages::TestStepFinished>& testStepFinished,
        const std::shared_ptr<const messages::TestStep>& testStep, const std::shared_ptr<const messages::PickleStep>& pickleStep,
        const std::shared_ptr<const messages::Step>& step) const
    {
        const auto status = testStepFinished->testStepResult->status;
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

    void StepFormatter::FormatLocationCommentTo(LineBuilder& lineBuilder, const std::shared_ptr<const messages::TestStep>& testStep) const
    {
        const auto& optUnambiguousStepDefinition = data.FindUnambiguousStepDefinitionBy(testStep);

        if (!optUnambiguousStepDefinition.has_value())
        {
            return;
        }

        const auto& sourceReference = optUnambiguousStepDefinition.value()->sourceReference;
        const auto& comment = sourceReferenceFormatter.Format(sourceReference);
        cucumber::pretty_formatter::FormatLocationCommentTo(lineBuilder, comment);
    }

    void StepFormatter::FormatLocationCommentTo(LineBuilder& lineBuilder, const std::shared_ptr<const messages::Hook>& hook) const
    {
        const auto& comment = sourceReferenceFormatter.Format(hook->sourceReference);
        cucumber::pretty_formatter::FormatLocationCommentTo(lineBuilder, comment);
    }
}
