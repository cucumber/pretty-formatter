package io.cucumber.prettyformatter;

import io.cucumber.messages.types.PickleDocString;

import static io.cucumber.prettyformatter.Theme.Element.DOC_STRING;
import static io.cucumber.prettyformatter.Theme.Element.DOC_STRING_CONTENT;
import static io.cucumber.prettyformatter.Theme.Element.DOC_STRING_CONTENT_TYPE;
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
        LineBuilder lineBuilder2 = lineBuilder.indent(indentation).begin(DOC_STRING);
        LineBuilder lineBuilder3 = lineBuilder2.append(Theme.Element.DOC_STRING_DELIMITER, DOC_STRING_DELIMITER);
        lineBuilder3.append(DOC_STRING_CONTENT_TYPE, printableMediaType).end(DOC_STRING)
                .newLine();

        // Doc strings are normalized to \n by Gherkin.
        String[] lines = pickleDocString.getContent().split("\\n");
        for (String line : lines) {
            LineBuilder lineBuilder1 = lineBuilder.indent(indentation).begin(DOC_STRING);
            lineBuilder1.append(DOC_STRING_CONTENT, line).end(DOC_STRING)
                    .newLine();
        }
        LineBuilder lineBuilder1 = lineBuilder.indent(indentation).begin(DOC_STRING);
        lineBuilder1.append(Theme.Element.DOC_STRING_DELIMITER, DOC_STRING_DELIMITER).end(DOC_STRING)
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
