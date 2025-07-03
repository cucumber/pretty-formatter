package io.cucumber.prettyformatter;

import io.cucumber.messages.types.PickleDocString;

import static java.lang.System.lineSeparator;
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

    String format(PickleDocString pickleDocString) {
        StringBuilder builder = new StringBuilder();
        String printableMediaType = pickleDocString.getMediaType().orElse("");
        builder.append(indentation)
                .append(DOC_STRING_DELIMITER)
                .append(printableMediaType)
                .append(lineSeparator());

        // Doc strings are normalized to \n by Gherkin.
        String[] lines = pickleDocString.getContent().split("\\n");
        for (String line : lines) {
            builder.append(indentation)
                    .append(line)
                    .append(lineSeparator());
        }
        builder.append(indentation)
                .append(DOC_STRING_DELIMITER)
                .append(lineSeparator());

        return builder.toString();
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
