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

        std::string FormatTagLine(const std::vector<std::shared_ptr<const messages::PickleTag>>& tags)
        {
            if (tags.empty())
            {
                return "";
            }

            auto str = tags.front()->name;
            for (auto it = std::next(tags.begin()); it != tags.end(); ++it)
            {
                str += " " + (*it)->name;
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

            if (envelope.testCaseStarted.has_value())
            {
                CalculateLocationIndent(envelope.testCaseStarted.value());
            }
        }

        std::size_t GetAfterFeatureIndent() const
        {
            return afterFeatureIndent;
        }

        std::size_t GetScenarioIndentBy(const std::shared_ptr<const messages::TestCaseStarted>& testCaseStarted) const
        {
            return GetScenarioIndentBy(testCaseStarted->id);
        }

        std::size_t GetScenarioIndentBy(const std::shared_ptr<const messages::TestCaseFinished>& testCaseFinished) const
        {
            return GetScenarioIndentBy(testCaseFinished->testCaseStartedId) + 0;
        }

        std::size_t GetScenarioIndentBy(const std::string& testCaseStartedId) const
        {
            return scenarioIndentByTestCaseStartedId.at(testCaseStartedId);
        }

        std::size_t GetCommentStartAtIndexBy(const std::shared_ptr<const messages::TestCaseStarted>& testCaseStarted) const
        {
            return GetCommentStartAtIndexBy(testCaseStarted->id);
        }

        std::size_t GetCommentStartAtIndexBy(const std::shared_ptr<const messages::TestStepFinished>& testStepFinished) const
        {
            return GetCommentStartAtIndexBy(testStepFinished->testCaseStartedId);
        }

        std::size_t GetStepIndentBy(const std::shared_ptr<const messages::TestStepFinished>& testStepFinished) const
        {
            return GetScenarioIndentBy(testStepFinished->testCaseStartedId) + stepIndent;
        }

        std::size_t GetCommentStartAtIndexBy(const std::string& testCaseStartedId) const
        {
            if (commentStartIndexByTestCaseStartedId.find(testCaseStartedId) != commentStartIndexByTestCaseStartedId.end())
            {
                return commentStartIndexByTestCaseStartedId.at(testCaseStartedId);
            }
            return 0;
        }

        std::size_t GetStackTraceIndentBy(const std::shared_ptr<const messages::TestStepFinished>& testStepFinished) const
        {
            return GetStepIndentBy(testStepFinished) + iconLength + afterStepStacktraceIndent;
        }

        std::size_t GetArgumentIndentBy(const std::shared_ptr<const messages::TestStepFinished>& testStepFinished) const
        {
            return GetStepIndentBy(testStepFinished) + iconLength + afterStepArgumentIndent;
        }

        std::size_t GetAttachmentIndentBy(const std::shared_ptr<const messages::Attachment>& attachment) const
        {
            if (attachment->testCaseStartedId.has_value())
            {
                return GetScenarioIndentBy(attachment->testCaseStartedId.value()) + afterScenarioAttachmentIndent + iconLength;
            }
            return afterScenarioAttachmentIndent + iconLength;
        }

        std::optional<std::vector<std::shared_ptr<const messages::PickleTag>>> FindTagsBy(
            const std::shared_ptr<const messages::TestCaseStarted>& testCaseStarted) const
        {
            const auto pickle = query.FindPickleBy(testCaseStarted);
            if (pickle.has_value())
            {
                return std::vector<std::shared_ptr<const messages::PickleTag>>{ pickle.value()->tags.begin(), pickle.value()->tags.end() };
            }

            return std::nullopt;
        }

        std::optional<std::shared_ptr<const messages::Scenario>> FindScenarioBy(std::shared_ptr<const messages::Pickle> pickle) const
        {
            const auto optLineage = query.FindLineageBy(pickle);
            if (optLineage.has_value())
            {
                return optLineage.value().lineage->scenario;
            }

            return std::nullopt;
        }

        std::optional<std::shared_ptr<const messages::SourceReference>> FindSourceReferenceBy(
            std::shared_ptr<const messages::TestStep> testStep) const
        {
            const auto optStepDefinition = query.FindUnambiguousStepDefinitionBy(testStep);
            if (optStepDefinition.has_value())
            {
                return optStepDefinition.value()->sourceReference;
            }
            return std::nullopt;
        }

        query::Query query;

        template<class TFunc>
        void IfNotSeen(std::shared_ptr<const messages::Feature> feature, TFunc&& func)
        {
            if (seenFeatures.find(feature) == seenFeatures.end())
            {
                seenFeatures.insert(feature);
                func();
            }
        }

        template<class TFunc>
        void IfNotSeen(std::shared_ptr<const messages::Rule> rule, TFunc&& func)
        {
            if (seenRules.find(rule) == seenRules.end())
            {
                seenRules.insert(rule);
                func();
            }
        }

    private:
        std::set<std::shared_ptr<const messages::Feature>> seenFeatures;
        std::set<std::shared_ptr<const messages::Rule>> seenRules;

        std::map<std::string, std::size_t> scenarioIndentByTestCaseStartedId;
        std::map<std::string, std::size_t> commentStartIndexByTestCaseStartedId;

    private:
        void CalculateLocationIndent(const std::shared_ptr<const messages::TestCaseStarted>& testCaseStarted)
        {
            const auto optPickle = query.FindPickleBy(testCaseStarted);
            if (optPickle.has_value())
            {
                const auto& pickle = optPickle.value();
                const auto optLineageAndPickle = query.FindLineageBy(pickle);

                if (optLineageAndPickle.has_value() && optLineageAndPickle.value().lineage->scenario)
                {
                    const auto& lineageAndPickle = optLineageAndPickle.value();
                    const auto& scenario = optLineageAndPickle.value().lineage->scenario;

                    const auto scenarioIndent = CalculateScenarioIndent(lineageAndPickle);
                    const auto scenarioLineLength = CalculateScenarioLineLength(scenarioIndent, *pickle, *scenario);
                    const auto longestLine = std::accumulate(pickle->steps.begin(), pickle->steps.end(), scenarioLineLength,
                        [&](std::size_t lhs, const std::shared_ptr<const messages::PickleStep>& rhs)
                        {
                            return std::max(lhs, CalculatePickleStepLineLength(scenarioIndent, rhs));
                        });

                    scenarioIndentByTestCaseStartedId[testCaseStarted->id] = scenarioIndent;
                    commentStartIndexByTestCaseStartedId[testCaseStarted->id] = longestLine + 1;
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

        std::size_t CalculatePickleStepLineLength(std::size_t scenarioIndent, const std::shared_ptr<const messages::PickleStep>& pickleStep)
        {
            const auto& step = query.FindStepBy(pickleStep);
            if (step.has_value())
            {
                return CalculateStepLineLength(scenarioIndent, step.value(), pickleStep);
            }
            return 0;
        }

        std::size_t CalculateStepLineLength(std::size_t scenarioIndent, const std::shared_ptr<const messages::Step>& step,
            const std::shared_ptr<const messages::PickleStep>& pickleStep) const
        {
            return scenarioIndent + stepIndent + iconLength + step->keyword.length() + pickleStep->text.length();
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

        void TestCaseStarted(std::shared_ptr<const messages::TestCaseStarted> testCaseStarted)
        {
            const auto optLineage = data->query.FindLineageBy(testCaseStarted);
            if (optLineage.has_value())
            {
                const auto& lineage = optLineage.value();

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

        void TestStepFinished(std::shared_ptr<const messages::TestStepFinished> testStepFinished)
        {
            PrintStep(testStepFinished);
            PrintAmbiguousStep(testStepFinished);
            PrintException(testStepFinished);
            stream.flush();
        }

        void TestRunFinished(std::shared_ptr<const messages::TestRunFinished> testRunFinished)
        {
            if (testRunFinished->exception.has_value())
            {
                const auto& exception = testRunFinished->exception.value();
                ExceptionFormatter formatter{ 0, theme, messages::TestStepResultStatus::FAILED };
                const auto optString = formatter.Format(exception);
                if (optString.has_value())
                {
                    fmt::print(stream, "{}", optString.value());
                }
            }
        }

        void Attachment(std::shared_ptr<const messages::Attachment> attachment)
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
        void PrintFeature(std::shared_ptr<const messages::Feature> feature)
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

        void PrintRule(std::shared_ptr<const messages::Rule> rule)
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

        void PrintTags(std::shared_ptr<const messages::TestCaseStarted> testCaseStarted)
        {
            const auto tags = data->FindTagsBy(testCaseStarted);
            if (tags.has_value() && !tags.value().empty())
            {
                fmt::println(stream, "{}",
                    LineBuilder{ theme }
                        .Indent(data->GetScenarioIndentBy(testCaseStarted)) //
                        .Append(Theme::Element::tag, FormatTagLine(tags.value()))
                        .Build());
            }
        }

        void PrintScenarioDefinition(std::shared_ptr<const messages::TestCaseStarted> testCaseStarted)
        {
            const auto optPickle = data->query.FindPickleBy(testCaseStarted);
            if (optPickle.has_value())
            {
                const auto optScenario = data->FindScenarioBy(optPickle.value());
                if (optScenario.has_value())
                {
                    fmt::println(stream, "{}", FormatScenarioLine(testCaseStarted, optPickle.value(), optScenario.value()));
                }
            }
        }

        std::string FormatScenarioLine(std::shared_ptr<const messages::TestCaseStarted> testCaseStarted,
            std::shared_ptr<const messages::Pickle> pickle, std::shared_ptr<const messages::Scenario> scenario)
        {
            return LineBuilder{ theme }
                .Indent(data->GetScenarioIndentBy(testCaseStarted)) //
                .Begin(Theme::Element::scenario)
                .Title(Theme::Element::scenarioKeyword, scenario->keyword, Theme::Element::scenarioName, pickle->name)
                .End(Theme::Element::scenario)
                .AddPaddingUpTo(data->GetCommentStartAtIndexBy(testCaseStarted))
                .Append(Theme::Element::location, "# " + FormatLocation(pickle))
                .Build();
        }

        void PrintStep(std::shared_ptr<const messages::TestStepFinished> testStepFinished)
        {
            const auto optTestStep = data->query.FindTestStepBy(testStepFinished);
            if (optTestStep.has_value())
            {
                const auto& testStep = optTestStep.value();

                const auto optPickleStep = data->query.FindPickleStepBy(testStep);
                if (optPickleStep.has_value())
                {
                    const auto& pickleStep = optPickleStep.value();
                    const auto optStep = data->query.FindStepBy(pickleStep);
                    if (optStep.has_value())
                    {
                        const auto& step = optStep.value();
                        fmt::println(stream, "{}", FormatStep(testStepFinished, testStep, pickleStep, step));

                        if (pickleStep->argument.has_value())
                        {
                            const auto& argument = pickleStep->argument.value();
                            if (argument->dataTable.has_value())
                            {
                                fmt::print(stream, "{}",
                                    LineBuilder{ theme }
                                        .Accept(
                                            [this, &testStepFinished, &argument](LineBuilder& lineBuilder)
                                            {
                                                PickleTableFormatter{ data->GetArgumentIndentBy(testStepFinished) }.Format(lineBuilder,
                                                    argument->dataTable.value());
                                            })
                                        .Build());
                            }
                            if (argument->docString.has_value())
                            {
                                fmt::print(stream, "{}",
                                    LineBuilder{ theme }
                                        .Accept(
                                            [this, &testStepFinished, &argument](LineBuilder& lineBuilder)
                                            {
                                                PickleDocStringFormatter{ data->GetArgumentIndentBy(testStepFinished) }.Format(lineBuilder,
                                                    argument->docString.value());
                                            })
                                        .Build());
                            }
                        }
                    }
                }
            }
        }

        void PrintAmbiguousStep(std::shared_ptr<const messages::TestStepFinished> testStepFinished)
        {
            if (testStepFinished->testStepResult->status == messages::TestStepResultStatus::AMBIGUOUS)
            {
                const auto optTestStep = data->query.FindTestStepBy(testStepFinished);
                if (optTestStep.has_value())
                {
                    const auto& testStep = optTestStep.value();
                    fmt::print(stream, "{}",
                        LineBuilder{ theme }
                            .Accept(
                                [this, &testStepFinished, &testStep](LineBuilder& lineBuilder)
                                {
                                    AmbiguousStepDefinitionsFormatter{ data->GetStackTraceIndentBy(testStepFinished), theme,
                                        sourceReferenceFormatter }
                                        .Format(lineBuilder, data->query.FindStepDefinitionsBy(testStep));
                                })
                            .Build());
                }
            }
        }

        void PrintException(std::shared_ptr<const messages::TestStepFinished> testStepFinished)
        {
            const auto indent = data->GetStackTraceIndentBy(testStepFinished);
            const auto testStepResult = testStepFinished->testStepResult;
            const auto status = testStepResult->status;
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
        }

        std::string FormatStep(std::shared_ptr<const messages::TestStepFinished> testStepFinished,
            const std::shared_ptr<const messages::TestStep>& testStep, const std::shared_ptr<const messages::PickleStep>& pickleStep,
            const std::shared_ptr<const messages::Step>& step)
        {
            const auto status = testStepFinished->testStepResult->status;
            return LineBuilder{ theme } //
                .Indent(data->GetStepIndentBy(testStepFinished))
                .Accept(
                    [this, &status](LineBuilder& lineBuilder)
                    {
                        FormatStatusIcon(lineBuilder, status);
                    })
                .Begin(Theme::Element::step, status)
                .Append(Theme::Element::stepKeyword, step->keyword)
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
                        if (optLocation.has_value())
                        {
                            lineBuilder.AddPaddingUpTo(data->GetCommentStartAtIndexBy(testStepFinished));
                            lineBuilder.Append(Theme::Element::location, "# " + optLocation.value());
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

        std::string FormatLocation(std::shared_ptr<const messages::Pickle> pickle)
        {
            auto uri = pickle->uri; // uriformatter
            const auto location = data->query.FindLocationOf(pickle);
            if (location.has_value())
            {
                return fmt::format("{}:{}", uri, location.value()->line);
            }
            return uri;
        }

        std::optional<std::string> FormatLocation(std::shared_ptr<const messages::TestStep> testStep)
        {
            const auto optSourceReference = data->FindSourceReferenceBy(testStep);
            if (optSourceReference.has_value())
            {
                return sourceReferenceFormatter.Format(optSourceReference.value());
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

        if (envelope.testCaseStarted.has_value())
        {
            printer->TestCaseStarted(envelope.testCaseStarted.value());
        }

        if (envelope.testStepFinished.has_value())
        {
            printer->TestStepFinished(envelope.testStepFinished.value());
        }

        if (envelope.testRunFinished.has_value())
        {
            printer->TestRunFinished(envelope.testRunFinished.value());
        }

        if (envelope.attachment.has_value())
        {
            printer->Attachment(envelope.attachment.value());
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
