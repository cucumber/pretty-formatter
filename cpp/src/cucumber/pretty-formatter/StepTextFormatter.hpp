#ifndef CUCUMBER_PRETTY_FORMATTER_STEP_TEXT_FORMATTER_HPP
#define CUCUMBER_PRETTY_FORMATTER_STEP_TEXT_FORMATTER_HPP

#include "cucumber/messages/PickleStep.hpp"
#include "cucumber/messages/StepMatchArgument.hpp"
#include "cucumber/messages/TestStep.hpp"
#include "cucumber/pretty-formatter/LineBuilder.hpp"
#include <string_view>
#include <vector>

namespace cucumber::pretty_formatter
{
    struct StepTextFormatter
    {
        void Format(LineBuilder& lineBuilder, const messages::TestStep& testStep, const messages::PickleStep& pickleStep) const;

    private:
        [[nodiscard]] std::vector<const messages::StepMatchArgument*> GetStepMatchArguments(const messages::TestStep& testStep) const;

        void Format(LineBuilder& lineBuilder, std::string_view stepText,
            const std::vector<const messages::StepMatchArgument*>& stepMatchArguments) const;
    };
}

#endif
