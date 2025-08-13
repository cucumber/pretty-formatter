package io.cucumber.prettyformatter;

import io.cucumber.messages.types.Envelope;

import java.io.IOException;
import java.io.OutputStream;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Function;

import static io.cucumber.prettyformatter.MessagesToPrettyWriter.PrettyFeature.INCLUDE_ATTACHMENTS;
import static io.cucumber.prettyformatter.MessagesToPrettyWriter.PrettyFeature.INCLUDE_FEATURE_LINE;
import static io.cucumber.prettyformatter.MessagesToPrettyWriter.PrettyFeature.INCLUDE_RULE_LINE;
import static io.cucumber.prettyformatter.MessagesToPrettyWriter.PrettyFeature.USE_STATUS_ICON;
import static java.util.Objects.requireNonNull;

/**
 * Writes a pretty report of the scenario execution as it happens.
 */
public final class MessagesToPrettyWriter implements AutoCloseable {

    private final PrettyReportData data;
    private final PrettyReportWriter writer;
    private boolean streamClosed = false;

    private MessagesToPrettyWriter(OutputStream out, Theme theme, Function<String, String> uriFormatter, Set<PrettyFeature> features) {
        this.data = new PrettyReportData(features);
        this.writer = new PrettyReportWriter(out, theme, uriFormatter, features, data);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Writes a cucumber message to the pretty output.
     *
     * @param envelope the message
     * @throws IOException if an IO error occurs
     */
    public void write(Envelope envelope) throws IOException {
        if (streamClosed) {
            throw new IOException("Stream closed");
        }
        data.update(envelope);
        envelope.getTestCaseStarted().ifPresent(writer::handleTestCaseStarted);
        envelope.getTestStepFinished().ifPresent(writer::handleTestStepFinished);
        envelope.getTestRunFinished().ifPresent(writer::handleTestRunFinished);
        envelope.getAttachment().ifPresent(writer::handleAttachment);
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

    public enum PrettyFeature {
        /**
         * Include feature lines.
         * <p>
         * When executing in parallel the feature and rules lines can make the
         * output even harder to read as they would typically all be emitted at
         * once. Excluding these can make the report more readable in these
         * circumstances.
         */
        INCLUDE_FEATURE_LINE,

        /**
         * Include rule lines.
         *
         * @see #INCLUDE_FEATURE_LINE
         */
        INCLUDE_RULE_LINE,

        /**
         * Adds a status icon next to each step line.
         */
        USE_STATUS_ICON,

        /**
         * Include attachment lines.
         */
        INCLUDE_ATTACHMENTS
    }

    public static final class Builder {

        private final EnumSet<PrettyFeature> features = EnumSet.of(
                INCLUDE_FEATURE_LINE,
                INCLUDE_RULE_LINE,
                USE_STATUS_ICON,
                INCLUDE_ATTACHMENTS
        );
        private Theme theme = Theme.none();
        private Function<String, String> uriFormatter = Function.identity();

        private Builder() {
        }

        private static Function<String, String> removePrefix(String prefix) {
            // TODO: Needs coverage
            return s -> {
                if (s.startsWith(prefix)) {
                    return s.substring(prefix.length());
                }
                return s;
            };
        }

        /**
         * Adds a theme to the pretty writer.
         */
        public Builder theme(Theme theme) {
            this.theme = requireNonNull(theme);
            return this;
        }

        /**
         * Removes a given prefix from all URI locations.
         * <p>
         * The typical usage would be to trim the current working directory.
         * This makes the report more readable.
         */
        public Builder removeUriPrefix(String prefix) {
            // TODO: Needs coverage
            this.uriFormatter = removePrefix(requireNonNull(prefix));
            return this;
        }

        /**
         * Toggles a given feature.
         */
        public Builder feature(PrettyFeature feature, boolean enabled) {
            if (enabled) {
                features.add(feature);
            } else {
                features.remove(feature);
            }
            return this;
        }

        public MessagesToPrettyWriter build(OutputStream out) {
            requireNonNull(out);
            Set<PrettyFeature> features = EnumSet.copyOf(this.features);
            if (!theme.hasStatusIcons()) {
                features.remove(USE_STATUS_ICON);
            }
            return new MessagesToPrettyWriter(out, theme, uriFormatter, features);
        }
    }

}
