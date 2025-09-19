package io.cucumber.prettyformatter;

import io.cucumber.messages.types.Envelope;

import java.io.IOException;
import java.io.OutputStream;

import static java.util.Objects.requireNonNull;

public final class MessagesToProgressWriter implements AutoCloseable {

    private final ProgressWriter writer;
    private boolean streamClosed = false;

    private MessagesToProgressWriter(OutputStream out, Theme theme, int maxWidth) {
        this.writer = new ProgressWriter(out, theme, maxWidth);
    }

    public static MessagesToProgressWriter.Builder builder() {
        return new MessagesToProgressWriter.Builder();
    }

    /**
     * Writes a cucumber message to the dot progress output.
     *
     * @param envelope the message
     * @throws IOException if an IO error occurs
     */
    public void write(Envelope envelope) throws IOException {
        if (streamClosed) {
            throw new IOException("Stream closed");
        }
        envelope.getTestRunHookFinished().ifPresent(writer::write);
        envelope.getTestStepFinished().ifPresent(writer::write);
        envelope.getTestRunFinished().ifPresent(writer::write);
    }

    /**
     * Closes the stream, flushing it first. Once closed further write()
     * invocations will cause an IOException to be thrown. Closing a closed
     * stream has no effect.
     */
    @Override
    public void close() {
        if (streamClosed) {
            return;
        }

        try {
            writer.close();
        } finally {
            streamClosed = true;
        }
    }


    public static final class Builder {

        private static final int DEFAULT_MAX_WIDTH = 80;
        // Without any progress icons, there is no output
        private Theme theme = Theme.plain();
        private int maxWidth = DEFAULT_MAX_WIDTH;

        private Builder() {
        }

        /**
         * Adds a theme to the progress writer.
         */
        public Builder theme(Theme theme) {
            this.theme = requireNonNull(theme);
            return this;
        }

        /**
         * Sets the max width in characters of a progress line.
         * <p>
         * Defaults to {@value DEFAULT_MAX_WIDTH}
         */
        public Builder maxWidth(int maxWidth) {
            if (maxWidth <= 0) {
                throw new IllegalArgumentException("maxWidth must be a positive non-zero value");
            }
            this.maxWidth = maxWidth;
            return this;
        }

        public MessagesToProgressWriter build(OutputStream out) {
            requireNonNull(out);
            return new MessagesToProgressWriter(out, theme, maxWidth);
        }
    }
}
