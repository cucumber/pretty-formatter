package io.cucumber.prettyformatter;

import io.cucumber.messages.types.PickleDocString;

import static java.util.Objects.requireNonNull;

final class PickleDocStringFormatter {

    private static final String DOC_STRING_DELIMITER = "\"\"\"";
    private final String indentation;

    private PickleDocStringFormatter(String indentation) {
        this.indentation = indentation;
    }

    static Builder builder() {
        return new Builder();
    }

    void formatTo(PickleDocString pickleDocString, LineBuilder lineBuilder) {
        String printableMediaType = pickleDocString.getMediaType().orElse("");
        lineBuilder.indent(indentation)
                .beginDocString()
                .docStringDelimiter(DOC_STRING_DELIMITER)
                .docStringContentType(printableMediaType)
                .endDocString()
                .newLine();

        // Doc strings are normalized to \n by Gherkin.
        String[] lines = pickleDocString.getContent().split("\\n");
        for (String line : lines) {
            lineBuilder.indent(indentation)
                    .beginDocString()
                    .docStringContent(line)
                    .endDocString()
                    .newLine();
        }
        lineBuilder.indent(indentation)
                .beginDocString()
                .docStringDelimiter(DOC_STRING_DELIMITER)
                .endDocString()
                .newLine();

    }

    static final class Builder {

        private String indentation = "";

        private Builder() {

        }

        Builder indentation(String indentation) {
            requireNonNull(indentation, "indentation may not be null");
            this.indentation = indentation;
            return this;
        }

        PickleDocStringFormatter build() {
            return new PickleDocStringFormatter(indentation);
        }
    }
}
