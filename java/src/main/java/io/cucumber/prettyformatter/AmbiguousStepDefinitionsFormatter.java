package io.cucumber.prettyformatter;

import io.cucumber.messages.types.StepDefinition;

import java.util.List;

import static io.cucumber.prettyformatter.Theme.Element.LOCATION;

final class AmbiguousStepDefinitionsFormatter {

    private final int indentation;
    private final SourceReferenceFormatter sourceReferenceFormatter;

    private AmbiguousStepDefinitionsFormatter(int indentation, SourceReferenceFormatter sourceReferenceFormatter) {
        this.indentation = indentation;
        this.sourceReferenceFormatter = sourceReferenceFormatter;
    }

    static Builder builder() {
        return new Builder();
    }

    void formatTo(List<StepDefinition> stepDefinitions, LineBuilder lineBuilder) {
        lineBuilder.indent(indentation)
                .append("Multiple matching step definitions found:")
                .newLine();
        for (StepDefinition stepDefinition : stepDefinitions) {
            lineBuilder.indent(indentation)
                    .append("  - ")
                    .append(stepDefinition.getPattern().getSource());
            sourceReferenceFormatter.format(stepDefinition.getSourceReference())
                    .ifPresent(location -> lineBuilder.append(" ").append(LOCATION, "# " + location));
            lineBuilder.newLine();
        }
    }

    static final class Builder {
        private int indentation = 0;
        private SourceReferenceFormatter sourceReferenceFormatter;

        private Builder() {
        }

        Builder indentation(int indentation) {
            this.indentation = indentation;
            return this;
        }

        Builder sourceReferenceFormatter(SourceReferenceFormatter sourceReferenceFormatter) {
            this.sourceReferenceFormatter = sourceReferenceFormatter;
            return this;
        }

        AmbiguousStepDefinitionsFormatter build() {
            return new AmbiguousStepDefinitionsFormatter(indentation, sourceReferenceFormatter);
        }
    }
}
