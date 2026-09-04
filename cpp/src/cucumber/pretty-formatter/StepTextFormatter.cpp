#include "cucumber/pretty-formatter/StepTextFormatter.hpp"
#include "cucumber/messages/PickleStep.hpp"
#include "cucumber/messages/StepMatchArgument.hpp"
#include "cucumber/messages/TestStep.hpp"
#include "cucumber/pretty-formatter/LineBuilder.hpp"
#include "cucumber/pretty-formatter/Theme.hpp"
#include <cstddef>
#include <memory>
#include <string>
#include <vector>

namespace cucumber::pretty_formatter
{
    void StepTextFormatter::Format(LineBuilder& lineBuilder, const std::shared_ptr<const messages::TestStep>& testStep,
        const std::shared_ptr<const messages::PickleStep>& pickleStep) const
    {
        Format(lineBuilder, pickleStep->text, GetStepMatchArguments(testStep));
    }

    std::vector<std::shared_ptr<const messages::StepMatchArgument>> StepTextFormatter::GetStepMatchArguments(
        const std::shared_ptr<const messages::TestStep>& testStep) const
    {
        std::vector<std::shared_ptr<const messages::StepMatchArgument>> stepMatchArguments;

        if (testStep->stepMatchArgumentsLists.has_value() && testStep->stepMatchArgumentsLists.value().size() == 1)
        {
            for (const auto& argumentsList : testStep->stepMatchArgumentsLists.value())
            {
                stepMatchArguments.insert(stepMatchArguments.end(), argumentsList->stepMatchArguments.begin(),
                    argumentsList->stepMatchArguments.end());
            }
        }

        return stepMatchArguments;
    }

    void StepTextFormatter::Format(LineBuilder& lineBuilder, std::string_view stepText,
        const std::vector<std::shared_ptr<const messages::StepMatchArgument>>& stepMatchArguments) const
    {
        std::size_t pos = 0;

        for (const auto& stepArgument : stepMatchArguments)
        {
            const auto& group = stepArgument->group;
            if (group->value.has_value() && group->start.has_value())
            {
                const auto groupValue = group->value.value();
                const auto groupStart = group->start.value();
                const auto text = stepText.substr(pos, groupStart - pos);

                pos = groupStart + groupValue.size();

                lineBuilder.Append(Theme::Element::stepText, text).Append(Theme::Element::stepArgument, groupValue);
            }
        }

        if (const auto size = stepText.size(); pos != size)
        {
            lineBuilder.Append(Theme::Element::stepText, stepText.substr(pos));
        }
    }
}
