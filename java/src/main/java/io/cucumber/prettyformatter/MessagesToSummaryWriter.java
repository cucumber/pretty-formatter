package io.cucumber.prettyformatter;

import io.cucumber.messages.types.Envelope;
import io.cucumber.query.Repository;

import java.io.IOException;
import java.io.OutputStream;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Function;

import static io.cucumber.query.Repository.RepositoryFeature;
import static io.cucumber.query.Repository.RepositoryFeature.INCLUDE_GHERKIN_DOCUMENTS;
import static io.cucumber.query.Repository.RepositoryFeature.INCLUDE_HOOKS;
import static io.cucumber.query.Repository.RepositoryFeature.INCLUDE_STEP_DEFINITIONS;
import static io.cucumber.query.Repository.RepositoryFeature.INCLUDE_SUGGESTIONS;
import static io.cucumber.query.Repository.RepositoryFeature.INCLUDE_UNDEFINED_PARAMETER_TYPES;
import static java.util.Objects.requireNonNull;

/**
 * Writes the summary output of a test run.
 * <p>
 * Note: Messages are first collected and only written once the stream is
 * closed.
 */
public final class MessagesToSummaryWriter implements AutoCloseable {

    private final Repository repository = Repository.builder()
            .feature(RepositoryFeature.INCLUDE_ATTACHMENTS, true)
            .feature(INCLUDE_HOOKS, true)
            .feature(INCLUDE_GHERKIN_DOCUMENTS, true)
            .feature(INCLUDE_STEP_DEFINITIONS, true)
            .feature(INCLUDE_SUGGESTIONS, true)
            .feature(INCLUDE_UNDEFINED_PARAMETER_TYPES, true)
            .build();
    private final OutputStream out;
    private final Theme theme;
    private final Function<String, String> uriFormatter;
    private final Set<SummaryFeature> features;
    private boolean streamClosed = false;

    private MessagesToSummaryWriter(OutputStream out, Theme theme, Function<String, String> uriFormatter, Set<SummaryFeature> features) {
        this.out = out;
        this.theme = theme;
        this.uriFormatter = uriFormatter;
        this.features = features;
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
        repository.update(envelope);
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
        
        try (SummaryReportWriter writer = new SummaryReportWriter(out, theme, uriFormatter, features, repository)){
            writer.printSummary();
        } finally {
            streamClosed = true;
        }
    }

    public enum SummaryFeature {
        /**
         * Include attachment lines.
         */
        INCLUDE_ATTACHMENTS
    }

    public static final class Builder {

        private final EnumSet<SummaryFeature> features = EnumSet.of(
                SummaryFeature.INCLUDE_ATTACHMENTS
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
         * Adds a theme to the summary writer.
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
        public Builder feature(SummaryFeature feature, boolean enabled) {
            if (enabled) {
                features.add(feature);
            } else {
                features.remove(feature);
            }
            return this;
        }

        public MessagesToSummaryWriter build(OutputStream out) {
            requireNonNull(out);
            Set<SummaryFeature> features = EnumSet.copyOf(this.features);
            return new MessagesToSummaryWriter(out, theme, uriFormatter, features);
        }
    }

}
