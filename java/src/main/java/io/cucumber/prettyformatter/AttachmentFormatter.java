package io.cucumber.prettyformatter;

import io.cucumber.messages.types.Attachment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import static io.cucumber.prettyformatter.Theme.Element.ATTACHMENT;

final class AttachmentFormatter {

    private final int indentation;

    private AttachmentFormatter(int indentation) {
        this.indentation = indentation;
    }

    static Builder builder() {
        return new Builder();
    }

    void formatTo(Attachment attachment, LineBuilder lineBuilder) {
        switch (attachment.getContentEncoding()) {
            case BASE64 -> formatBase64Attachment(attachment, lineBuilder);
            case IDENTITY -> formatTextAttachment(attachment, lineBuilder);
        }
    }

    private void formatBase64Attachment(Attachment attachment, LineBuilder lineBuilder) {
        int bytes = (attachment.getBody().length() / 4) * 3;
        String line = attachment.getFileName().isPresent() //
                ? "Embedding %s [%s %d bytes]".formatted(attachment.getFileName().get(), attachment.getMediaType(), bytes) //
                : "Embedding [%s %d bytes]".formatted(attachment.getMediaType(), bytes);

        lineBuilder.indent(indentation)
                .append(ATTACHMENT, line)
                .newLine();
    }

    private void formatTextAttachment(Attachment attachment, LineBuilder lineBuilder) {
        // Prevent interleaving when multiple threads write to System.out
        try (BufferedReader lines = new BufferedReader(new StringReader(attachment.getBody()))) {
            String line;
            while ((line = lines.readLine()) != null) {
                lineBuilder.indent(indentation)
                        .append(ATTACHMENT, line)
                        .newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static final class Builder {
        private int indentation = 0;

        private Builder() {
        }

        Builder indentation(int indentation) {
            this.indentation = indentation;
            return this;
        }

        AttachmentFormatter build() {
            return new AttachmentFormatter(indentation);
        }
    }
}
