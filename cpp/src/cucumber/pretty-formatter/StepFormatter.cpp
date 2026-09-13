#include "cucumber/pretty-formatter/StepFormatter.hpp"
#include "cucumber/messages/Hook.hpp"
#include "cucumber/messages/PickleStep.hpp"
#include "cucumber/messages/Step.hpp"
#include "cucumber/messages/TestCaseFinished.hpp"
#include "cucumber/messages/TestStep.hpp"
#include "cucumber/messages/TestStepFinished.hpp"
#include "cucumber/messages/TestStepResultStatus.hpp"
#include "cucumber/pretty-formatter/AmbiguousStepDefinitionsFormatter.hpp"
#include "cucumber/pretty-formatter/AttachmentFormatter.hpp"
#include "cucumber/pretty-formatter/FormatResultException.hpp"
#include "cucumber/pretty-formatter/HookTypeName.hpp"
#include "cucumber/pretty-formatter/LineBuilder.hpp"
#include "cucumber/pretty-formatter/LocationComment.hpp"
#include "cucumber/pretty-formatter/SourceReferenceFormatter.hpp"
#include "cucumber/pretty-formatter/Theme.hpp"
#include "cucumber/query/Query.hpp"
#include <cstddef>
#include <fmt/core.h>
#include <fmt/format.h>
#include <fmt/ostream.h>
#include <memory>
#include <sstream>
#include <string>
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
                const auto status = testStepFinished->testStepResult.status;
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

    std::string StepFormatter::FormatNonPassingSteps(const messages::TestCaseFinished& testCaseFinished)
    {
        const auto* testCaseStarted = data.FindTestCaseStartedBy(testCaseFinished);
        if (testCaseStarted == nullptr)
        {
            return "";
        }

        const auto allTestStepFinishedAndTestStep = data.FindTestStepFinishedAndTestStepBy(*testCaseStarted);
        if (allTestStepFinishedAndTestStep.empty())
        {
            return "";
        }

        std::string result;
        for (const auto& [testStepFinished, testStep] : FindNonPassingSteps(allTestStepFinishedAndTestStep))
        {
            result += FormatStep(*testStepFinished, *testStep);
        }

        return result;
    }

    std::string StepFormatter::FormatStep(const messages::TestStepFinished& testStepFinished, const messages::TestStep& testStep)
    {
        std::ostringstream stream;

        const auto status = testStepFinished.testStepResult.status;
        const auto* pickleStep = data.FindPickleStepBy(testStep);

        if (pickleStep != nullptr)
        {
            const auto* step = data.FindStepBy(*pickleStep);
            if (step != nullptr)
            {
                fmt::println(stream, "{}", FormatPickleStep(testStepFinished, testStep, *pickleStep, *step));

                if (pickleStep->argument)
                {
                    if (pickleStep->argument->dataTable)
                    {
                        fmt::print(stream, "{}",
                            LineBuilder{ theme }
                                .Accept(
                                    [this, &pickleStep](LineBuilder& lineBuilder)
                                    {
                                        pickleTableFormatter.Format(lineBuilder, *pickleStep->argument->dataTable);
                                    })
                                .Build());
                    }

                    if (pickleStep->argument->docString)
                    {
                        fmt::print(stream, "{}",
                            LineBuilder{ theme }
                                .Accept(
                                    [this, &pickleStep](LineBuilder& lineBuilder)
                                    {
                                        pickleDocStringFormatter.Format(lineBuilder, *pickleStep->argument->docString);
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

        const auto* hook = data.FindHookBy(testStep);
        if (hook != nullptr)
        {
            fmt::println(stream, "{}", FormatHookStep(testStepFinished, *hook));
        }

        fmt::print(stream, "{}", FormatResultException(testStepFinished.testStepResult, deepIndent, theme));

        if (includeAttachments)
        {
            const auto attachments = data.FindAttachmentsBy(testStepFinished);
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

    std::string StepFormatter::FormatHookStep(const messages::TestStepFinished& testStepFinished, const messages::Hook& hook) const
    {
        const auto& status = testStepFinished.testStepResult.status;
        return LineBuilder{ theme }
            .Indent(indent)
            .Begin(Theme::Element::step, status)
            .Append(Theme::Element::stepKeyword, HookTypeName(hook.type))
            .Append(hook.name.has_value() ? "(" + hook.name.value() + ")" : "")
            .End(Theme::Element::step, status)
            .Accept(
                [this, &hook](auto& lineBuilder)
                {
                    AppendLocationComment(lineBuilder, sourceReferenceFormatter, hook.sourceReference);
                })
            .Build();
    }

    std::string StepFormatter::FormatPickleStep(const messages::TestStepFinished& testStepFinished, const messages::TestStep& testStep,
        const messages::PickleStep& pickleStep, const messages::Step& step) const
    {
        const auto status = testStepFinished.testStepResult.status;
        return LineBuilder{ theme }
            .Indent(indent)
            .Begin(Theme::Element::step, status)
            .Append(Theme::Element::stepKeyword, step.keyword)
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

    void StepFormatter::FormatLocationCommentTo(LineBuilder& lineBuilder, const messages::TestStep& testStep) const
    {
        const auto* unambiguousStepDefinition = data.FindUnambiguousStepDefinitionBy(testStep);

        if (unambiguousStepDefinition == nullptr)
        {
            return;
        }

        AppendLocationComment(lineBuilder, sourceReferenceFormatter, unambiguousStepDefinition->sourceReference);
    }
}
