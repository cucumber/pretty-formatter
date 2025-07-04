package io.cucumber.prettyformatter;

import io.cucumber.messages.types.PickleDocString;

import static io.cucumber.prettyformatter.Theme.Element.DOC_STRING;
import static io.cucumber.prettyformatter.Theme.Element.DOC_STRING_CONTENT;
import static io.cucumber.prettyformatter.Theme.Element.DOC_STRING_MEDIA_TYPE;
import static io.cucumber.prettyformatter.Theme.Element.DOC_STRING_DELIMITER;
import static java.util.Objects.requireNonNull;

final class PickleDocStringFormatter {

    private static final String DOC_STRING_DELIMITER_STRING = "\"\"\"";
    private final String indentation;

    private PickleDocStringFormatter(String indentation) {
        this.indentation = indentation;
    }

    static Builder builder() {
        return new Builder();
    }

    void formatTo(PickleDocString pickleDocString, LineBuilder lineBuilder) {
        lineBuilder
                .indent(indentation)
                .begin(DOC_STRING)
                .append(DOC_STRING_DELIMITER, DOC_STRING_DELIMITER_STRING)
                .accept(lb -> pickleDocString.getMediaType().ifPresent(mediaType -> lb.append(DOC_STRING_MEDIA_TYPE, mediaType)))
                .end(DOC_STRING)
                .newLine();

        // Doc strings are normalized to \n by Gherkin.
        String[] lines = pickleDocString.getContent().split("\\n");
        for (String line : lines) {
            lineBuilder.indent(indentation).
                    begin(DOC_STRING)
                    .append(DOC_STRING_CONTENT, line)
                    .end(DOC_STRING)
                    .newLine();
        }
        lineBuilder
                .indent(indentation)
                .begin(DOC_STRING).
                append(DOC_STRING_DELIMITER, DOC_STRING_DELIMITER_STRING)
                .end(DOC_STRING)
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
