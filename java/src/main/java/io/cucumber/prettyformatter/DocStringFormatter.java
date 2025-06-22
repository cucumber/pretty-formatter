package io.cucumber.prettyformatter;

import io.cucumber.messages.types.PickleDocString;

import java.io.IOException;

import static java.util.Objects.requireNonNull;

// TODO: Move?
final class DocStringFormatter {

    private final String indentation;

    private DocStringFormatter(String indentation) {
        this.indentation = indentation;
    }

    public static Builder builder() {
        return new Builder();
    }

    void formatTo(Appendable out, PickleDocString docString) throws IOException {
        String printableMediaType = docString.getMediaType().orElse("");
        out.append(indentation).append("\"\"\"").append(printableMediaType).append(System.lineSeparator());
        for (String l : docString.getContent().split("\\n")) {
            out.append(indentation).append(l).append(System.lineSeparator());
        }
        out.append(indentation).append("\"\"\"").append(System.lineSeparator());

    }

    public String format(PickleDocString pickleDocString) {
        StringBuilder builder = new StringBuilder();
        try {
            formatTo(builder, pickleDocString);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return builder.toString();
    }

    static final class Builder {

        private String indentation = "";

        private Builder() {

        }

        public Builder indentation(String indentation) {
            requireNonNull(indentation, "indentation may not be null");
            this.indentation = indentation;
            return this;
        }

        public DocStringFormatter build() {
            return new DocStringFormatter(indentation);
        }
    }
}
