#ifndef CUCUMBER_PRETTY_FORMATTER_SUMMARY_PRINTER_HPP
#define CUCUMBER_PRETTY_FORMATTER_SUMMARY_PRINTER_HPP

#include "cucumber/messages/Duration.hpp"
#include "cucumber/messages/Envelope.hpp"
#include "cucumber/messages/Exception.hpp"
#include "cucumber/messages/Pickle.hpp"
#include "cucumber/messages/PickleStep.hpp"
#include "cucumber/messages/Step.hpp"
#include "cucumber/messages/TestCaseFinished.hpp"
#include "cucumber/messages/TestCaseStarted.hpp"
#include "cucumber/messages/TestStep.hpp"
#include "cucumber/messages/TestStepFinished.hpp"
#include "cucumber/messages/TestStepResultStatus.hpp"
#include "cucumber/pretty-formatter/Formatter.hpp"
#include "cucumber/pretty-formatter/LineBuilder.hpp"
#include "cucumber/pretty-formatter/PickleDocStringFormatter.hpp"
#include "cucumber/pretty-formatter/PickleTableFormatter.hpp"
#include "cucumber/pretty-formatter/SourceReferenceFormatter.hpp"
#include "cucumber/pretty-formatter/StepTextFormatter.hpp"
#include "cucumber/pretty-formatter/Theme.hpp"
#include "cucumber/query/Query.hpp"
#include <cstdint>
#include <fmt/core.h>
#include <fmt/format.h>
#include <fmt/ostream.h>
#include <functional>
#include <map>
#include <memory>
#include <optional>
#include <ostream>
#include <set>
#include <string>
#include <vector>

namespace cucumber::pretty_formatter
{
    struct SummaryPrinter : Formatter
    {
        enum class Options : std::uint8_t
        {
            includeFeatureLine,
            includeRuleLine,
            useStatusIcon,
            includeAttachments
        };

    protected:
        struct ProtectedConstructorTag
        {};

    public:
        SummaryPrinter(const ProtectedConstructorTag&, std::ostream& stream, std::shared_ptr<struct Theme> theme,
            std::function<std::string(std::string)> uriFormatter, std::set<enum Options> options);

        void Update(const messages::Envelope& envelope) override;

        struct Factory
        {
            Factory();

            Factory& Theme(std::shared_ptr<struct Theme> theme);
            Factory& RemoveUriPrefix(std::string prefix);
            Factory& Options(enum Options option, bool enabled = true);

            std::unique_ptr<Formatter> Build(std::ostream& stream) const;

        private:
            std::shared_ptr<struct Theme> theme;
            std::function<std::string(std::string)> uriFormatter;
            std::set<enum Options> options;
        };

    private:
        void PrintSummary();

        void PrintNonPassingScenarios();

        template<class T, class U, class V>
        void PrintFinishedItemByStatus(std::string finishedItemname,
            std::map<messages::TestStepResultStatus, std::vector<T>> finishedItemByStatus, messages::TestStepResultStatus status,
            U&& formatFinishedItem, V&& printSupplementaryContent);

        void PrintUnknownParameterTypes();
        void PrintNonPassingGlobalHooks();
        void PrintNonPassingTestRun();
        void PrintStats();

        void PrintTestRunCount();
        void PrintGlobalHookCount();
        void PrintScenarioCounts();
        void PrintStepCounts();
        void PrintDurations();

        void PrintSnippets();

        void PrintStep(const std::shared_ptr<const messages::TestStepFinished>& testStepFinished,
            const std::shared_ptr<const messages::TestStep>& testStep);

        messages::TestStepResultStatus GetTestStepResultStatusBy(const std::shared_ptr<const messages::TestCaseFinished>& testCaseFinished);
        std::optional<std::shared_ptr<const messages::Exception>> GetTestRunWithException() const;

        std::shared_ptr<const messages::Duration> GetExecutionDuration() const;

        void FormatScenarioLineTo(const std::shared_ptr<const messages::TestCaseFinished>& testCaseFinished, LineBuilder& lineBuilder);
        void PrintNonPassingSteps(const std::shared_ptr<const messages::TestCaseFinished>& testCaseFinished,
            messages::TestStepResultStatus ignoredStatus);

        void FormatLocationCommentTo(LineBuilder& lineBuilder, const std::shared_ptr<const messages::Pickle>& pickle) const;
        void FormatLocationCommentTo(LineBuilder& lineBuilder, const std::shared_ptr<const messages::TestStep>& testStep) const;
        void FormatLocationCommentTo(LineBuilder& lineBuilder, const std::shared_ptr<const messages::Hook>& hook) const;
        void FormatLocationCommentTo(LineBuilder& lineBuilder, const std::optional<std::string>& comment) const;
        void FormatLocationCommentTo(LineBuilder& lineBuilder, const std::string& comment) const;

        std::string FormatAttempt(const std::shared_ptr<const messages::TestCaseStarted>& testCaseStarted) const;

        std::string FormatPickleStep(const std::shared_ptr<const messages::TestStepFinished>& testStepFinished,
            const std::shared_ptr<const messages::TestStep>& testStep, const std::shared_ptr<const messages::PickleStep>& pickleStep,
            const std::shared_ptr<const messages::Step>& step);

        std::ostream& stream; // NOLINT(cppcoreguidelines-avoid-const-or-ref-data-members) : ostream isn't copyable
        std::shared_ptr<struct Theme> theme;
        std::function<std::string(std::string)> uriFormatter;
        std::set<enum Options> options;

        query::Query query;

        StepTextFormatter stepTextFormatter;
        SourceReferenceFormatter sourceReferenceFormatter{ uriFormatter };

        constexpr static auto argumentIndent{ 9 };
        PickleTableFormatter pickleTableFormatter{ argumentIndent };
        PickleDocStringFormatter pickleDocStringFormatter{ argumentIndent };
    };
}

#endif
