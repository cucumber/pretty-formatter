#include "cucumber/pretty-formatter/AmbiguousStepDefinitionsFormatter.hpp"
#include "cucumber/messages/StepDefinition.hpp"
#include "cucumber/pretty-formatter/LineBuilder.hpp"
#include "cucumber/pretty-formatter/SourceReferenceFormatter.hpp"
#include "cucumber/pretty-formatter/Theme.hpp"
#include <cstddef>
#include <memory>
#include <string>
#include <utility>
#include <vector>

namespace cucumber::pretty_formatter
{
    AmbiguousStepDefinitionsFormatter::AmbiguousStepDefinitionsFormatter(std::shared_ptr<Theme> theme,
        SourceReferenceFormatter sourceReferenceFormatter)
        : AmbiguousStepDefinitionsFormatter{ 0, std::move(theme), std::move(sourceReferenceFormatter) }
    {}

    AmbiguousStepDefinitionsFormatter::AmbiguousStepDefinitionsFormatter(std::size_t indent, std::shared_ptr<Theme> theme,
        SourceReferenceFormatter sourceReferenceFormatter)
        : indent{ indent }
        , theme{ std::move(theme) }
        , sourceReferenceFormatter{ std::move(sourceReferenceFormatter) }
    {}

    void AmbiguousStepDefinitionsFormatter::Format(LineBuilder& lineBuilder,
        const std::vector<std::shared_ptr<const messages::StepDefinition>>& stepDefinitions)
    {
        lineBuilder.Indent(indent).Append("Multiple matching step definitions found:").NewLine();

        for (const auto& stepDefinition : stepDefinitions)
        {
            lineBuilder.Indent(indent).Append("  ").Append(theme->BulletPointIcon()).Append(" ").Append(stepDefinition->pattern->source);

            const auto optUri = sourceReferenceFormatter.Format(stepDefinition->sourceReference);
            if (optUri.has_value())
            {
                lineBuilder.Append(" ").Append(Theme::Element::location, "# " + optUri.value());
            }
            lineBuilder.NewLine();
        }
    }
}
