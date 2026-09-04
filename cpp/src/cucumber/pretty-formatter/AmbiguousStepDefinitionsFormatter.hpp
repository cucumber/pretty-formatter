#ifndef CUCUMBER_PRETTY_FORMATTER_AMBIGUOUS_STEP_DEFINITIONS_FORMATTER_HPP
#define CUCUMBER_PRETTY_FORMATTER_AMBIGUOUS_STEP_DEFINITIONS_FORMATTER_HPP

#include "cucumber/messages/StepDefinition.hpp"
#include "cucumber/pretty-formatter/LineBuilder.hpp"
#include "cucumber/pretty-formatter/SourceReferenceFormatter.hpp"
#include "cucumber/pretty-formatter/Theme.hpp"
#include <cstddef>
#include <memory>
#include <vector>

namespace cucumber::pretty_formatter
{
    struct AmbiguousStepDefinitionsFormatter
    {
        AmbiguousStepDefinitionsFormatter(std::shared_ptr<Theme> theme, SourceReferenceFormatter sourceReferenceFormatter);

        AmbiguousStepDefinitionsFormatter(std::size_t indent, std::shared_ptr<Theme> theme,
            SourceReferenceFormatter sourceReferenceFormatter);

        void Format(LineBuilder& lineBuilder, const std::vector<std::shared_ptr<const messages::StepDefinition>>& stepDefinitions);

    private:
        std::size_t indent;
        std::shared_ptr<Theme> theme;
        SourceReferenceFormatter sourceReferenceFormatter;
    };
}

#endif
