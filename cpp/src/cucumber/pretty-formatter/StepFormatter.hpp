#ifndef CUCUMBER_PRETTY_FORMATTER_STEP_FORMATTER_HPP
#define CUCUMBER_PRETTY_FORMATTER_STEP_FORMATTER_HPP

#include "cucumber/messages/Hook.hpp"
#include "cucumber/messages/PickleStep.hpp"
#include "cucumber/messages/Step.hpp"
#include "cucumber/messages/TestCaseFinished.hpp"
#include "cucumber/messages/TestStep.hpp"
#include "cucumber/messages/TestStepFinished.hpp"
#include "cucumber/pretty-formatter/LineBuilder.hpp"
#include "cucumber/pretty-formatter/PickleDocStringFormatter.hpp"
#include "cucumber/pretty-formatter/PickleTableFormatter.hpp"
#include "cucumber/pretty-formatter/SourceReferenceFormatter.hpp"
#include "cucumber/pretty-formatter/StepTextFormatter.hpp"
#include "cucumber/pretty-formatter/Theme.hpp"
#include "cucumber/query/Query.hpp"
#include <cstddef>
#include <memory>
#include <optional>
#include <string>

namespace cucumber::pretty_formatter
{
    struct StepFormatter
    {
        StepFormatter(query::Query& data, std::shared_ptr<Theme> theme, SourceReferenceFormatter sourceReferenceFormatter,
            std::size_t indent, bool includeAttachments);

        ~StepFormatter() = default;

        StepFormatter(const StepFormatter&) = delete;
        StepFormatter(StepFormatter&&) = delete;

        StepFormatter& operator=(const StepFormatter&) = delete;
        StepFormatter& operator=(StepFormatter&&) = delete;

        [[nodiscard]] std::string FormatNonPassingSteps(const std::shared_ptr<const messages::TestCaseFinished>& testCaseFinished);

        [[nodiscard]] std::string FormatStep(const std::shared_ptr<const messages::TestStepFinished>& testStepFinished,
            const std::shared_ptr<const messages::TestStep>& testStep);

    private:
        [[nodiscard]] std::string FormatPickleStep(const std::shared_ptr<const messages::TestStepFinished>& testStepFinished,
            const std::shared_ptr<const messages::TestStep>& testStep, const std::shared_ptr<const messages::PickleStep>& pickleStep,
            const std::shared_ptr<const messages::Step>& step) const;

        [[nodiscard]] std::string FormatHookStep(const std::shared_ptr<const messages::TestStepFinished>& testStepFinished,
            const std::shared_ptr<const messages::Hook>& hook) const;

        void FormatLocationCommentTo(LineBuilder& lineBuilder, const std::shared_ptr<const messages::TestStep>& testStep) const;

        query::Query& data;
        std::shared_ptr<Theme> theme;
        SourceReferenceFormatter sourceReferenceFormatter;
        std::size_t indent;
        std::size_t argumentIndent;
        std::size_t deepIndent;
        bool includeAttachments;

        StepTextFormatter stepTextFormatter;
        PickleTableFormatter pickleTableFormatter{ argumentIndent };
        PickleDocStringFormatter pickleDocStringFormatter{ argumentIndent };
    };
}

#endif
