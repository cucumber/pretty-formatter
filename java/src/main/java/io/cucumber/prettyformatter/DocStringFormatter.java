package io.cucumber.prettyformatter;

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

    void formatTo(Appendable out, String printableContentType, String content) throws IOException {
        out.append(indentation).append("\"\"\"").append(printableContentType).append("\n");
        for (String l : content.split("\\n")) {
            out.append(indentation).append(l).append("\n");
        }
        out.append(indentation).append("\"\"\"").append("\n");
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
