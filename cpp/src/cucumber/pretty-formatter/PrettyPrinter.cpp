#include "cucumber/pretty-formatter/PrettyPrinter.hpp"
#include "cucumber/messages/Attachment.hpp"
#include "cucumber/messages/AttachmentContentEncoding.hpp"
#include "cucumber/messages/Envelope.hpp"
#include "cucumber/messages/Exception.hpp"
#include "cucumber/messages/Feature.hpp"
#include "cucumber/messages/Pickle.hpp"
#include "cucumber/messages/PickleDocString.hpp"
#include "cucumber/messages/PickleStep.hpp"
#include "cucumber/messages/PickleTable.hpp"
#include "cucumber/messages/PickleTag.hpp"
#include "cucumber/messages/Rule.hpp"
#include "cucumber/messages/Scenario.hpp"
#include "cucumber/messages/SourceReference.hpp"
#include "cucumber/messages/Step.hpp"
#include "cucumber/messages/StepDefinition.hpp"
#include "cucumber/messages/StepMatchArgument.hpp"
#include "cucumber/messages/TestCaseFinished.hpp"
#include "cucumber/messages/TestCaseStarted.hpp"
#include "cucumber/messages/TestRunFinished.hpp"
#include "cucumber/messages/TestStep.hpp"
#include "cucumber/messages/TestStepFinished.hpp"
#include "cucumber/messages/TestStepResultStatus.hpp"
#include "cucumber/pretty-formatter/AmbiguousStepDefinitionsFormatter.hpp"
#include "cucumber/pretty-formatter/AttachmentFormatter.hpp"
#include "cucumber/pretty-formatter/ExceptionFormatter.hpp"
#include "cucumber/pretty-formatter/Formatter.hpp"
#include "cucumber/pretty-formatter/LineBuilder.hpp"
#include "cucumber/pretty-formatter/PickleDocStringFormatter.hpp"
#include "cucumber/pretty-formatter/PickleTableFormatter.hpp"
#include "cucumber/pretty-formatter/SourceReferenceFormatter.hpp"
#include "cucumber/pretty-formatter/StepTextFormatter.hpp"
#include "cucumber/pretty-formatter/Theme.hpp"
#include "cucumber/query/Query.hpp"
#include "fmt/core.h"
#include "fmt/ostream.h"
#include <algorithm>
#include <cstddef>
#include <fmt/format.h>
#include <functional>
#include <iterator>
#include <map>
#include <memory>
#include <numeric>
#include <optional>
#include <ostream>
#include <set>
#include <sstream>
#include <string>
#include <utility>
#include <vector>

namespace cucumber::pretty_formatter
{
    namespace
    {
        constexpr std::size_t visualStatusIconLength{ 1 };
        constexpr std::size_t afterScenarioAttachmentIndent{ 6 };
        constexpr std::size_t afterStepStacktraceIndent{ 4 };
        constexpr std::size_t afterStepArgumentIndent{ 2 };
        constexpr std::size_t stepIndent{ 2 };
        constexpr std::size_t oneSpaceLength{ 1 };

        std::string FormatTagLine(const std::vector<messages::PickleTag>& tags)
        {
            if (tags.empty())
            {
                return "";
            }

            auto str = tags.front().name;
            for (auto it = std::next(tags.begin()); it != tags.end(); ++it)
            {
                str += " " + it->name;
            }
            return str;
        }
    }

    struct PrettyPrinter::Data
    {
        explicit Data(std::set<PrettyPrinter::Options> options)
            : options{ std::move(options) }
        {}

        void Update(const messages::Envelope& envelope)
        {
            query.Update(envelope);

            if (envelope.testCaseStarted)
            {
                CalculateLocationIndent(*envelope.testCaseStarted);
            }
        }

        std::size_t GetAfterFeatureIndent() const
        {
            return afterFeatureIndent;
        }

        std::size_t GetScenarioIndentBy(const messages::TestCaseStarted& testCaseStarted) const
        {
            return GetScenarioIndentBy(testCaseStarted.id);
        }

        std::size_t GetScenarioIndentBy(const messages::TestCaseFinished& testCaseFinished) const
        {
            return GetScenarioIndentBy(testCaseFinished.testCaseStartedId) + 0;
        }

        std::size_t GetScenarioIndentBy(const std::string& testCaseStartedId) const
        {
            return scenarioIndentByTestCaseStartedId.at(testCaseStartedId);
        }

        std::size_t GetCommentStartAtIndexBy(const messages::TestCaseStarted& testCaseStarted) const
        {
            return GetCommentStartAtIndexBy(testCaseStarted.id);
        }

        std::size_t GetCommentStartAtIndexBy(const messages::TestStepFinished& testStepFinished) const
        {
            return GetCommentStartAtIndexBy(testStepFinished.testCaseStartedId);
        }

        std::size_t GetStepIndentBy(const messages::TestStepFinished& testStepFinished) const
        {
            return GetScenarioIndentBy(testStepFinished.testCaseStartedId) + stepIndent;
        }

        std::size_t GetCommentStartAtIndexBy(const std::string& testCaseStartedId) const
        {
            if (commentStartIndexByTestCaseStartedId.find(testCaseStartedId) != commentStartIndexByTestCaseStartedId.end())
            {
                return commentStartIndexByTestCaseStartedId.at(testCaseStartedId);
            }
            return 0;
        }

        std::size_t GetStackTraceIndentBy(const messages::TestStepFinished& testStepFinished) const
        {
            return GetStepIndentBy(testStepFinished) + iconLength + afterStepStacktraceIndent;
        }

        std::size_t GetArgumentIndentBy(const messages::TestStepFinished& testStepFinished) const
        {
            return GetStepIndentBy(testStepFinished) + iconLength + afterStepArgumentIndent;
        }

        std::size_t GetAttachmentIndentBy(const messages::Attachment& attachment) const
        {
            if (attachment.testCaseStartedId)
            {
                return GetScenarioIndentBy(*attachment.testCaseStartedId) + afterScenarioAttachmentIndent + iconLength;
            }
            return afterScenarioAttachmentIndent + iconLength;
        }

        std::optional<std::vector<messages::PickleTag>> FindTagsBy(const messages::TestCaseStarted& testCaseStarted) const
        {
            const auto* pickle = query.FindPickleBy(testCaseStarted);
            if (pickle != nullptr)
            {
                return std::vector<messages::PickleTag>{ pickle->tags.begin(), pickle->tags.end() };
            }

            return std::nullopt;
        }

        const messages::Scenario* FindScenarioBy(const messages::Pickle& pickle) const
        {
            const auto optLineage = query.FindLineageBy(pickle);
            if (optLineage)
            {
                return optLineage->lineage->scenario;
            }

            return nullptr;
        }

        const messages::SourceReference* FindSourceReferenceBy(const messages::TestStep& testStep) const
        {
            const auto* stepDefinition = query.FindUnambiguousStepDefinitionBy(testStep);
            if (stepDefinition != nullptr)
            {
                return std::addressof(stepDefinition->sourceReference);
            }
            return nullptr;
        }

        query::Query query;

        template<class TFunc>
        void IfNotSeen(const messages::Feature* feature, TFunc&& func)
        {
            if (seenFeatures.find(feature) == seenFeatures.end())
            {
                seenFeatures.insert(feature);
                func();
            }
        }

        template<class TFunc>
        void IfNotSeen(const messages::Rule* rule, TFunc&& func)
        {
            if (seenRules.find(rule) == seenRules.end())
            {
                seenRules.insert(rule);
                func();
            }
        }

    private:
        std::set<const messages::Feature*> seenFeatures;
        std::set<const messages::Rule*> seenRules;

        std::map<std::string, std::size_t> scenarioIndentByTestCaseStartedId;
        std::map<std::string, std::size_t> commentStartIndexByTestCaseStartedId;

    private:
        void CalculateLocationIndent(const messages::TestCaseStarted& testCaseStarted)
        {
            const auto* pickle = query.FindPickleBy(testCaseStarted);
            if (pickle != nullptr)
            {
                const auto optLineageAndPickle = query.FindLineageBy(*pickle);

                if (optLineageAndPickle && optLineageAndPickle->lineage->scenario)
                {
                    const auto& lineageAndPickle = *optLineageAndPickle;
                    const auto& scenario = optLineageAndPickle->lineage->scenario;

                    const auto scenarioIndent = CalculateScenarioIndent(lineageAndPickle);
                    const auto scenarioLineLength = CalculateScenarioLineLength(scenarioIndent, *pickle, *scenario);
                    const auto longestLine = std::accumulate(pickle->steps.begin(), pickle->steps.end(), scenarioLineLength,
                        [&](std::size_t lhs, const messages::PickleStep& rhs)
                        {
                            return std::max(lhs, CalculatePickleStepLineLength(scenarioIndent, rhs));
                        });

                    scenarioIndentByTestCaseStartedId[testCaseStarted.id] = scenarioIndent;
                    commentStartIndexByTestCaseStartedId[testCaseStarted.id] = longestLine + 1;
                }
            }
        }

        std::size_t CalculateScenarioIndent(const query::LineageAndPickle& lineageAndPickle) const
        {
            if (lineageAndPickle.lineage->rule)
            {
                return afterRuleIndent;
            }

            if (lineageAndPickle.lineage->feature)
            {
                return afterFeatureIndent;
            }

            return 0;
        }

        std::size_t CalculateScenarioLineLength(std::size_t scenarioIndent, const messages::Pickle& pickle,
            const messages::Scenario& scenario) const
        {
            return scenarioIndent + scenario.keyword.size() + 2 + pickle.name.size();
        }

        std::size_t CalculatePickleStepLineLength(std::size_t scenarioIndent, const messages::PickleStep& pickleStep)
        {
            const auto* step = query.FindStepBy(pickleStep);
            if (step != nullptr)
            {
                return CalculateStepLineLength(scenarioIndent, *step, pickleStep);
            }
            return 0;
        }

        std::size_t CalculateStepLineLength(std::size_t scenarioIndent, const messages::Step& step,
            const messages::PickleStep& pickleStep) const
        {
            return scenarioIndent + stepIndent + iconLength + step.keyword.length() + pickleStep.text.length();
        }

        std::set<PrettyPrinter::Options> options;

        std::size_t afterFeatureIndent{ [this]() -> std::size_t
            {
                if (options.find(PrettyPrinter::Options::includeFeatureLine) != options.end())
                {
                    return 2;
                }
                return 0;
            }() };

        std::size_t afterRuleIndent{ [this]() -> std::size_t
            {
                std::size_t indent = 0;
                if (options.find(PrettyPrinter::Options::includeFeatureLine) != options.end())
                {
                    indent += 2;
                }
                if (options.find(PrettyPrinter::Options::includeRuleLine) != options.end())
                {
                    indent += 2;
                }
                return indent;
            }() };

        std::size_t iconLength{ [this]() -> std::size_t
            {
                if (options.find(PrettyPrinter::Options::useStatusIcon) != options.end())
                {
                    return visualStatusIconLength + oneSpaceLength;
                }
                return 0;
            }() };
    };

    struct PrettyPrinter::Printer
    {
        Printer(std::ostream& stream, std::shared_ptr<Theme> theme, std::shared_ptr<Data> data,
            std::function<std::string(std::string)> uriFormatter, std::set<PrettyPrinter::Options> options)
            : stream{ stream }
            , theme{ std::move(theme) }
            , data{ std::move(data) }
            , uriFormatter{ std::move(uriFormatter) }
            , options{ std::move(options) }
        {}

        void TestCaseStarted(const messages::TestCaseStarted& testCaseStarted)
        {
            const auto optLineage = data->query.FindLineageBy(testCaseStarted);
            if (optLineage)
            {
                const auto& lineage = *optLineage;

                if (options.find(PrettyPrinter::Options::includeFeatureLine) != options.end() && lineage.lineage->feature)
                {
                    PrintFeature(lineage.lineage->feature);
                }

                if (options.find(PrettyPrinter::Options::includeRuleLine) != options.end() && lineage.lineage->rule)
                {
                    PrintRule(lineage.lineage->rule);
                }
            }

            fmt::println(stream, "");

            PrintTags(testCaseStarted);
            PrintScenarioDefinition(testCaseStarted);

            stream.flush();
        }

        void TestStepFinished(const messages::TestStepFinished& testStepFinished)
        {
            PrintStep(testStepFinished);
            PrintAmbiguousStep(testStepFinished);
            PrintException(testStepFinished);
            stream.flush();
        }

        void TestRunFinished(const messages::TestRunFinished& testRunFinished)
        {
            if (testRunFinished.exception)
            {
                const auto& exception = *testRunFinished.exception;
                ExceptionFormatter formatter{ 0, theme, messages::TestStepResultStatus::FAILED };
                const auto optString = formatter.Format(exception);
                if (optString)
                {
                    fmt::print(stream, "{}", *optString);
                }
            }
        }

        void Attachment(const messages::Attachment& attachment)
        {
            if (options.find(PrettyPrinter::Options::includeAttachments) == options.end())
            {
                return;
            }

            fmt::println(stream, "{}",
                LineBuilder{ theme }
                    .NewLine()
                    .Accept(
                        [this, &attachment](auto& lineBuilder)
                        {
                            AttachmentFormatter{ data->GetAttachmentIndentBy(attachment) }.Format(lineBuilder, attachment);
                        })
                    .Build());
            stream.flush();
        }

    private:
        void PrintFeature(const messages::Feature* feature)
        {
            data->IfNotSeen(feature,
                [this, &feature]
                {
                    fmt::println(stream, "{}",
                        LineBuilder{ theme }
                            .NewLine()
                            .Begin(Theme::Element::feature)
                            .Title(Theme::Element::featureKeyword, feature->keyword, Theme::Element::featureName, feature->name)
                            .End(Theme::Element::feature)
                            .Build());
                });
        }

        void PrintRule(const messages::Rule* rule)
        {
            data->IfNotSeen(rule,
                [this, &rule]
                {
                    fmt::println(stream, "{}",
                        LineBuilder{ theme }
                            .NewLine()
                            .Indent(data->GetAfterFeatureIndent())
                            .Begin(Theme::Element::rule)
                            .Title(Theme::Element::ruleKeyword, rule->keyword, Theme::Element::ruleName, rule->name)
                            .End(Theme::Element::rule)
                            .Build());
                });
        }

        void PrintTags(const messages::TestCaseStarted& testCaseStarted)
        {
            const auto tags = data->FindTagsBy(testCaseStarted);
            if (tags && !tags->empty())
            {
                fmt::println(stream, "{}",
                    LineBuilder{ theme }
                        .Indent(data->GetScenarioIndentBy(testCaseStarted)) //
                        .Append(Theme::Element::tag, FormatTagLine(*tags))
                        .Build());
            }
        }

        void PrintScenarioDefinition(const messages::TestCaseStarted& testCaseStarted)
        {
            const auto* pickle = data->query.FindPickleBy(testCaseStarted);
            if (pickle != nullptr)
            {
                const auto* scenario = data->FindScenarioBy(*pickle);
                if (scenario != nullptr)
                {
                    fmt::println(stream, "{}", FormatScenarioLine(testCaseStarted, *pickle, *scenario));
                }
            }
        }

        std::string FormatScenarioLine(const messages::TestCaseStarted& testCaseStarted, const messages::Pickle& pickle,
            const messages::Scenario& scenario)
        {
            return LineBuilder{ theme }
                .Indent(data->GetScenarioIndentBy(testCaseStarted)) //
                .Begin(Theme::Element::scenario)
                .Title(Theme::Element::scenarioKeyword, scenario.keyword, Theme::Element::scenarioName, pickle.name)
                .End(Theme::Element::scenario)
                .AddPaddingUpTo(data->GetCommentStartAtIndexBy(testCaseStarted))
                .Append(Theme::Element::location, "# " + FormatLocation(pickle))
                .Build();
        }

        void PrintStep(const messages::TestStepFinished& testStepFinished)
        {
            const auto* testStep = data->query.FindTestStepBy(testStepFinished);
            if (testStep != nullptr)
            {
                const auto* pickleStep = data->query.FindPickleStepBy(*testStep);
                if (pickleStep != nullptr)
                {
                    const auto* step = data->query.FindStepBy(*pickleStep);
                    if (step != nullptr)
                    {
                        fmt::println(stream, "{}", FormatStep(testStepFinished, *testStep, *pickleStep, *step));

                        if (pickleStep->argument)
                        {
                            const auto& argument = *pickleStep->argument;
                            if (argument.dataTable)
                            {
                                fmt::print(stream, "{}",
                                    LineBuilder{ theme }
                                        .Accept(
                                            [this, &testStepFinished, &argument](LineBuilder& lineBuilder)
                                            {
                                                PickleTableFormatter{ data->GetArgumentIndentBy(testStepFinished) }.Format(lineBuilder,
                                                    *argument.dataTable);
                                            })
                                        .Build());
                            }
                            if (argument.docString)
                            {
                                fmt::print(stream, "{}",
                                    LineBuilder{ theme }
                                        .Accept(
                                            [this, &testStepFinished, &argument](LineBuilder& lineBuilder)
                                            {
                                                PickleDocStringFormatter{ data->GetArgumentIndentBy(testStepFinished) }.Format(lineBuilder,
                                                    *argument.docString);
                                            })
                                        .Build());
                            }
                        }
                    }
                }
            }
        }

        void PrintAmbiguousStep(const messages::TestStepFinished& testStepFinished)
        {
            if (testStepFinished.testStepResult.status == messages::TestStepResultStatus::AMBIGUOUS)
            {
                const auto* testStep = data->query.FindTestStepBy(testStepFinished);
                if (testStep != nullptr)
                {
                    fmt::print(stream, "{}",
                        LineBuilder{ theme }
                            .Accept(
                                [this, &testStepFinished, &testStep](LineBuilder& lineBuilder)
                                {
                                    AmbiguousStepDefinitionsFormatter{ data->GetStackTraceIndentBy(testStepFinished), theme,
                                        sourceReferenceFormatter }
                                        .Format(lineBuilder, data->query.FindStepDefinitionsBy(*testStep));
                                })
                            .Build());
                }
            }
        }

        void PrintException(const messages::TestStepFinished& testStepFinished)
        {
            const auto indent = data->GetStackTraceIndentBy(testStepFinished);
            const auto& testStepResult = testStepFinished.testStepResult;
            const auto status = testStepResult.status;
            ExceptionFormatter exceptionFormatter{ indent, theme, status };
            const auto message = testStepResult.message;

            if (testStepResult.exception)
            {
                fmt::print(stream, "{}", exceptionFormatter.Format(*testStepResult.exception, message).value_or(""));
            }
            else if (message)
            {
                fmt::print(stream, "{}", exceptionFormatter.Format(*message));
            }
        }

        std::string FormatStep(const messages::TestStepFinished& testStepFinished, const messages::TestStep& testStep,
            const messages::PickleStep& pickleStep, const messages::Step& step)
        {
            const auto status = testStepFinished.testStepResult.status;
            return LineBuilder{ theme } //
                .Indent(data->GetStepIndentBy(testStepFinished))
                .Accept(
                    [this, &status](LineBuilder& lineBuilder)
                    {
                        FormatStatusIcon(lineBuilder, status);
                    })
                .Begin(Theme::Element::step, status)
                .Append(Theme::Element::stepKeyword, step.keyword)
                .Accept(
                    [this, &testStep, &pickleStep](LineBuilder& lineBuilder)
                    {
                        stepTextFormatter.Format(lineBuilder, testStep, pickleStep);
                    })
                .End(Theme::Element::step, status)
                .Accept(
                    [this, &testStepFinished, &testStep](LineBuilder& lineBuilder)
                    {
                        const auto optLocation = FormatLocation(testStep);
                        if (optLocation)
                        {
                            lineBuilder.AddPaddingUpTo(data->GetCommentStartAtIndexBy(testStepFinished));
                            lineBuilder.Append(Theme::Element::location, "# " + *optLocation);
                        }
                    })
                .Build();
        }

        void FormatStatusIcon(LineBuilder& lineBuilder, messages::TestStepResultStatus status)
        {
            if (options.find(PrettyPrinter::Options::useStatusIcon) != options.end())
            {
                lineBuilder.Begin(Theme::Element::statusIcon, status)
                    .StatusIcon(theme->StatusIcon(status))
                    .End(Theme::Element::statusIcon, status)
                    .Append(" ");
            }
        }

        std::string FormatLocation(const messages::Pickle& pickle)
        {
            auto uri = pickle.uri; // uriformatter
            const auto* location = data->query.FindLocationOf(pickle);
            if (location != nullptr)
            {
                return fmt::format("{}:{}", uri, location->line);
            }
            return uri;
        }

        std::optional<std::string> FormatLocation(const messages::TestStep& testStep)
        {
            const auto* sourceReference = data->FindSourceReferenceBy(testStep);
            if (sourceReference != nullptr)
            {
                return sourceReferenceFormatter.Format(*sourceReference);
            }
            return std::nullopt;
        }

        std::ostream& stream; // NOLINT(cppcoreguidelines-avoid-const-or-ref-data-members) : ostream isn't copyable
        std::shared_ptr<Theme> theme;
        std::shared_ptr<Data> data;
        std::function<std::string(std::string)> uriFormatter;
        std::set<PrettyPrinter::Options> options;

        SourceReferenceFormatter sourceReferenceFormatter{ uriFormatter };
        StepTextFormatter stepTextFormatter;
    };

    PrettyPrinter::PrettyPrinter(const ProtectedConstructorTag&, std::ostream& stream, std::shared_ptr<Theme> theme,
        std::function<std::string(std::string)> uriFormatter, std::set<Options> options)
        : theme{ std::move(theme) }
        , data{ std::make_shared<Data>(options) }
        , printer{ std::make_unique<Printer>(stream, this->theme, data, std::move(uriFormatter), std::move(options)) }
    {}

    PrettyPrinter::~PrettyPrinter() = default;

    void PrettyPrinter::Update(const messages::Envelope& envelope)
    {
        data->Update(envelope);

        if (envelope.testCaseStarted)
        {
            printer->TestCaseStarted(*envelope.testCaseStarted);
        }

        if (envelope.testStepFinished)
        {
            printer->TestStepFinished(*envelope.testStepFinished);
        }

        if (envelope.testRunFinished)
        {
            printer->TestRunFinished(*envelope.testRunFinished);
        }

        if (envelope.attachment)
        {
            printer->Attachment(*envelope.attachment);
        }
    }

    PrettyPrinter::Factory::Factory()
        : theme{ Theme::None() }
        , uriFormatter{ [](std::string uri)
            {
                return uri;
            } }
        , options{ Options::includeFeatureLine, Options::includeRuleLine, Options::useStatusIcon, Options::includeAttachments }
    {}

    PrettyPrinter::Factory& PrettyPrinter::Factory::Theme(std::shared_ptr<struct Theme> theme)
    {
        this->theme = std::move(theme);
        return *this;
    }

    PrettyPrinter::Factory& PrettyPrinter::Factory::RemoveUriPrefix(std::string prefix)
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

    PrettyPrinter::Factory& PrettyPrinter::Factory::Options(PrettyPrinter::Options option, bool enabled)
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

    std::unique_ptr<Formatter> PrettyPrinter::Factory::Build(std::ostream& stream)
    {
        if (!theme->HasStatusIcons())
        {
            options.erase(Options::useStatusIcon);
        }

        return std::make_unique<PrettyPrinter>(PrettyPrinter::ProtectedConstructorTag{}, stream, theme, uriFormatter, options);
    }
}
