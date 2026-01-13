package io.cucumber.prettyformatter;

import io.cucumber.messages.types.StepDefinition;

import java.util.List;

import static io.cucumber.prettyformatter.Theme.Element.LOCATION;

final class AmbiguousStepDefinitionsFormatter {

    private final int indentation;
    private final SourceReferenceFormatter sourceReferenceFormatter;
    private final Theme theme;

    private AmbiguousStepDefinitionsFormatter(Theme theme, int indentation, SourceReferenceFormatter sourceReferenceFormatter) {
        this.theme = theme;
        this.indentation = indentation;
        this.sourceReferenceFormatter = sourceReferenceFormatter;
    }

    static Builder builder(SourceReferenceFormatter sourceReferenceFormatter, Theme theme) {
        return new Builder(sourceReferenceFormatter, theme);
    }

    void formatTo(List<StepDefinition> stepDefinitions, LineBuilder lineBuilder) {
        lineBuilder.indent(indentation)
                .append("Multiple matching step definitions found:")
                .newLine();
        for (StepDefinition stepDefinition : stepDefinitions) {
            lineBuilder.indent(indentation)
                    .append("  ")
                    .append(theme.bulletPointIcon())
                    .append(" ")
                    .append(stepDefinition.getPattern().getSource());
            sourceReferenceFormatter.format(stepDefinition.getSourceReference())
                    .ifPresent(location -> lineBuilder.append(" ").append(LOCATION, "# " + location));
            lineBuilder.newLine();
        }
    }

    static final class Builder {
        private final SourceReferenceFormatter sourceReferenceFormatter;
        private final Theme theme;
        private int indentation = 0;

        private Builder(SourceReferenceFormatter sourceReferenceFormatter, Theme theme) {
            this.sourceReferenceFormatter = sourceReferenceFormatter;
            this.theme = theme;
        }

        Builder indentation(int indentation) {
            this.indentation = indentation;
            return this;
        }

        AmbiguousStepDefinitionsFormatter build() {
            return new AmbiguousStepDefinitionsFormatter(theme, indentation, sourceReferenceFormatter);
        }
    }
}
