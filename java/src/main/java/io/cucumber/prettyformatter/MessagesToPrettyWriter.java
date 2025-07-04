package io.cucumber.prettyformatter;

import io.cucumber.messages.types.Attachment;
import io.cucumber.messages.types.Envelope;
import io.cucumber.messages.types.Exception;
import io.cucumber.messages.types.Feature;
import io.cucumber.messages.types.Group;
import io.cucumber.messages.types.Location;
import io.cucumber.messages.types.Pickle;
import io.cucumber.messages.types.PickleStep;
import io.cucumber.messages.types.PickleTag;
import io.cucumber.messages.types.Rule;
import io.cucumber.messages.types.Scenario;
import io.cucumber.messages.types.SourceReference;
import io.cucumber.messages.types.Step;
import io.cucumber.messages.types.StepMatchArgument;
import io.cucumber.messages.types.StepMatchArgumentsList;
import io.cucumber.messages.types.TestCaseStarted;
import io.cucumber.messages.types.TestRunFinished;
import io.cucumber.messages.types.TestStep;
import io.cucumber.messages.types.TestStepFinished;
import io.cucumber.messages.types.TestStepResultStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static io.cucumber.messages.types.TestStepResultStatus.FAILED;
import static io.cucumber.prettyformatter.Theme.Element.ATTACHMENT;
import static io.cucumber.prettyformatter.Theme.Element.FEATURE;
import static io.cucumber.prettyformatter.Theme.Element.FEATURE_KEYWORD;
import static io.cucumber.prettyformatter.Theme.Element.FEATURE_NAME;
import static io.cucumber.prettyformatter.Theme.Element.LOCATION;
import static io.cucumber.prettyformatter.Theme.Element.RULE;
import static io.cucumber.prettyformatter.Theme.Element.RULE_KEYWORD;
import static io.cucumber.prettyformatter.Theme.Element.RULE_NAME;
import static io.cucumber.prettyformatter.Theme.Element.SCENARIO_KEYWORD;
import static io.cucumber.prettyformatter.Theme.Element.SCENARIO_NAME;
import static io.cucumber.prettyformatter.Theme.Element.STEP;
import static io.cucumber.prettyformatter.Theme.Element.STEP_ARGUMENT;
import static io.cucumber.prettyformatter.Theme.Element.STEP_KEYWORD;
import static io.cucumber.prettyformatter.Theme.Element.STEP_TEXT;
import static io.cucumber.prettyformatter.Theme.Element.TAG;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * Writes a pretty report of the scenario execution as it happens.
 */
public final class MessagesToPrettyWriter implements AutoCloseable {

    private final Theme theme;
    private final Function<String, String> uriFormatter;
    private final PrintWriter writer;
    private final PrettyReportData data;
    private final boolean includeFeatureAndRules;
    private boolean streamClosed = false;
    private Feature currentFeature;
    private Rule currentRule;

    public MessagesToPrettyWriter(OutputStream out) {
        this(out, Theme.cucumberJvm(), Function.identity(), false);
    }

    private MessagesToPrettyWriter(OutputStream out, Theme theme, Function<String, String> uriFormatter, boolean includeFeatureAndRules) {
        this.theme = requireNonNull(theme);
        this.writer = createPrintWriter(requireNonNull(out));
        this.uriFormatter = requireNonNull(uriFormatter);
        this.includeFeatureAndRules = includeFeatureAndRules;
        this.data = new PrettyReportData(includeFeatureAndRules);
    }

    public static Builder builder() {
        return new Builder();
    }

    private static PrintWriter createPrintWriter(OutputStream out) {
        return new PrintWriter(
                new OutputStreamWriter(
                        requireNonNull(out),
                        StandardCharsets.UTF_8
                )
        );
    }

    /**
     * Writes a cucumber message to the xml output.
     *
     * @param envelope the message
     * @throws IOException if an IO error occurs
     */
    public void write(Envelope envelope) throws IOException {
        if (streamClosed) {
            throw new IOException("Stream closed");
        }
        data.collect(envelope);
        envelope.getTestCaseStarted().ifPresent(this::handleTestCaseStarted);
        envelope.getTestStepFinished().ifPresent(this::handleTestStepFinished);
        envelope.getTestRunFinished().ifPresent(this::handleTestRunFinished);
        envelope.getAttachment().ifPresent(this::handleAttachment);
    }

    private void handleTestCaseStarted(TestCaseStarted event) {
        if (includeFeatureAndRules) {
            data.findLineageBy(event, this).ifPresent(lineage -> {
                lineage.feature().ifPresent(this::printFeature);
                lineage.rule().ifPresent(this::printRule);
            });
        }

        writer.println();
        printTags(event);
        printScenarioDefinition(event);
        writer.flush();
    }

    private void printFeature(Feature feature) {
        if (feature.equals(currentFeature)) {
            return;
        }
        this.currentFeature = feature;
        writer.println(new LineBuilder(theme)
                .begin(FEATURE)
                .title(FEATURE_KEYWORD, feature.getKeyword(), FEATURE_NAME, feature.getName())
                .end(FEATURE)
                .build());
    }

    private void printRule(Rule rule) {
        if (rule.equals(currentRule)) {
            return;
        }
        this.currentRule = rule;
        writer.println(new LineBuilder(theme)
                .newLine()
                .indent("  ")
                .begin(RULE)
                .title(RULE_KEYWORD, rule.getKeyword(), RULE_NAME, rule.getName())
                .end(RULE)
                .build());
    }

    private void printTags(TestCaseStarted event) {
        data.findTagsBy(event)
                .map(pickleTags -> new LineBuilder(theme)
                        .indent(data.getScenarioIndentBy(event))
                        .append(TAG, formatTagLine(pickleTags))
                        .build())
                .ifPresent(writer::println);
    }

    private String formatTagLine(List<PickleTag> pickleTags) {
        return pickleTags.stream()
                .map(PickleTag::getName)
                .collect(joining(" "));
    }

    private void printScenarioDefinition(TestCaseStarted event) {
        data.findPickleBy(event).ifPresent(pickle ->
                data.findScenarioBy(pickle).ifPresent(scenario ->
                        writer.println(formatScenarioLine(event, pickle, scenario))));
    }

    private String formatScenarioLine(TestCaseStarted event, Pickle pickle, Scenario scenario) {
        return new LineBuilder(theme)
                .indent(data.getScenarioIndentBy(event))
                .title(SCENARIO_KEYWORD, scenario.getKeyword(), SCENARIO_NAME, pickle.getName())
                .addPaddingUpTo(data.getCommentStartAtIndexBy(event))
                .append(LOCATION, "# " + formatLocation(pickle))
                .build();
    }

    private String formatLocation(Pickle pickle) {
        String path = uriFormatter.apply(pickle.getUri());
        return data.findLineOf(pickle)
                .map(line -> path + ":" + line)
                .orElse(path);
    }

    private void handleTestStepFinished(TestStepFinished event) {
        printStep(event);
        printException(event);
        writer.flush();
    }

    private void printStep(TestStepFinished event) {
        data.findTestStepBy(event).ifPresent(testStep ->
                data.findPickleStepBy(testStep).ifPresent(pickleStep ->
                        data.findStepBy(pickleStep).ifPresent(step -> {
                            writer.println(formatStep(event, testStep, pickleStep, step));
                            pickleStep.getArgument().ifPresent(pickleStepArgument -> {
                                pickleStepArgument.getDataTable().ifPresent(pickleTable ->
                                        writer.print(new LineBuilder(theme)
                                                .accept(lineBuilder -> PickleTableFormatter.builder()
                                                        .indentation(data.getArgumentIndentBy(event))
                                                        .build()
                                                        .formatTo(pickleTable, lineBuilder))
                                                .build())
                                );
                                pickleStepArgument.getDocString().ifPresent(pickleDocString ->
                                        writer.print(new LineBuilder(theme)
                                                .accept(lineBuilder -> PickleDocStringFormatter.builder()
                                                        .indentation(data.getArgumentIndentBy(event))
                                                        .build()
                                                        .formatTo(pickleDocString, lineBuilder))
                                                .build())
                                );
                            });
                        })));
    }

    private String formatStep(TestStepFinished event, TestStep testStep, PickleStep pickleStep, Step step) {
        TestStepResultStatus status = event.getTestStepResult().getStatus();
        return new LineBuilder(theme)
                .indent(data.getStepIndentBy(event))
                .begin(STEP, status)
                .append(STEP_KEYWORD, step.getKeyword())
                .accept(lineBuilder -> formatStepText(lineBuilder, testStep, pickleStep))
                .end(STEP, status)
                .accept(lineBuilder -> formatLocation(testStep)
                        .ifPresent(location ->
                                lineBuilder.addPaddingUpTo(data.getCommentStartAtIndexBy(event))
                                        .append(LOCATION, "# " + location)
                        )
                )
                .build();
    }

    private void formatStepText(LineBuilder line, TestStep testStep, PickleStep pickleStep) {
        List<StepMatchArgument> stepMatchArguments = testStep.getStepMatchArgumentsLists()
                .map(stepMatchArgumentsLists -> stepMatchArgumentsLists.stream()
                        .map(StepMatchArgumentsList::getStepMatchArguments)
                        .flatMap(Collection::stream)
                        .collect(toList())
                )
                .orElseGet(Collections::emptyList);

        formatStepText(line, pickleStep.getText(), stepMatchArguments);
    }

    void formatStepText(LineBuilder lineBuilder, String stepText, List<StepMatchArgument> arguments) {
        int beginIndex = 0;
        for (StepMatchArgument argument : arguments) {
            // can be null if the argument is missing.
            Group group = argument.getGroup();
            Optional<String> value = group.getValue();
            if (value.isPresent()) {
                // TODO: Messages are silly
                int argumentOffset = (int) (long) group.getStart().orElse(-1L);
                String text = stepText.substring(beginIndex, argumentOffset);
                int argumentEndIndex = argumentOffset + value.get().length();
                beginIndex = argumentEndIndex;
                lineBuilder.append(STEP_TEXT, text)
                        .append(STEP_ARGUMENT, stepText.substring(argumentOffset, argumentEndIndex));
            }
        }
        if (beginIndex != stepText.length()) {
            lineBuilder.append(STEP_TEXT, stepText.substring(beginIndex));
        }
    }

    private Optional<String> formatLocation(TestStep testStep) {
        return data.findSourceReferenceBy(testStep)
                .flatMap(this::formatSourceReference);
    }

    private Optional<String> formatSourceReference(SourceReference sourceReference) {
        // TODO: Needs coverage
        // TODO: can we do this lazy/functional?
        if (sourceReference.getJavaMethod().isPresent()) {
            return sourceReference.getJavaMethod()
                    .map(javaMethod -> String.format(
                            "%s.%s(%s)",
                            javaMethod.getClassName(),
                            javaMethod.getMethodName(),
                            String.join(",", javaMethod.getMethodParameterTypes())
                    ));
        }
        if (sourceReference.getJavaStackTraceElement().isPresent()) {
            return sourceReference.getJavaStackTraceElement()
                    .map(javaStackTraceElement -> String.format(
                            "%s.%s(%s%s)",
                            javaStackTraceElement.getClassName(),
                            javaStackTraceElement.getMethodName(),
                            javaStackTraceElement.getFileName(),
                            sourceReference.getLocation().map(Location::getLine).map(line -> ":" + line).orElse("")
                    ));
        }
        if (sourceReference.getUri().isPresent()) {
            return sourceReference.getUri()
                    .map(uri -> uriFormatter.apply(uri) + sourceReference.getLocation()
                            .map(location -> ":" + location.getLine())
                            .orElse(""));
        }
        return Optional.empty();
    }

    private void printException(TestStepFinished event) {
        TestStepResultStatus status = event.getTestStepResult().getStatus();
        event.getTestStepResult().getException().ifPresent(exception ->
                writer.println(formatError(data.getStackTraceIndentBy(event), exception, status)));
    }

    private void handleAttachment(Attachment attachment) {
        writer.println();
        switch (attachment.getContentEncoding()) {
            case BASE64:
                writer.println(formatBase64Attachment(attachment));
                break;
            case IDENTITY:
                writer.print(formatTextAttachment(attachment));
                break;
        }
        writer.println();
        writer.flush();
    }

    private String formatBase64Attachment(Attachment event) {
        int bytes = (event.getBody().length() / 4) * 3;
        String line;
        if (event.getFileName().isPresent()) {
            line = String.format("Embedding %s [%s %d bytes]", event.getFileName().get(), event.getMediaType(), bytes);
        } else {
            line = String.format("Embedding [%s %d bytes]", event.getMediaType(), bytes);
        }
        return new LineBuilder(theme)
                .indent(data.getAttachmentIndentBy(event))
                .append(ATTACHMENT, line)
                .build();
    }

    private String formatTextAttachment(Attachment event) {
        String indent = data.getAttachmentIndentBy(event);
        // Prevent interleaving when multiple threads write to System.out
        LineBuilder builder = new LineBuilder(theme);
        try (BufferedReader lines = new BufferedReader(new StringReader(event.getBody()))) {
            String line;
            while ((line = lines.readLine()) != null) {
                builder.indent(indent)
                        .append(ATTACHMENT, line)
                        .newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return builder.build();
    }

    private void handleTestRunFinished(TestRunFinished event) {
        printException(event);
        writer.close();
    }

    private void printException(TestRunFinished event) {
        event.getException().ifPresent(exception ->
                writer.println(formatError("", exception, FAILED)));
    }

    private String formatError(String scenarioIndent, Exception exception, TestStepResultStatus status) {
        String text = exception.getStackTrace().orElseGet(() -> exception.getMessage().orElse(""));
        // TODO: Java 12+ use String.indent
        String indented = text.replaceAll("(\r\n|\r|\n)", "$1" + scenarioIndent).trim();
        return new LineBuilder(theme)
                .indent(scenarioIndent)
                .error(status, indented)
                .build();
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

        private Theme theme = Theme.cucumberJvm();
        private Function<String, String> uriFormatter = Function.identity();
        private boolean includeFeatureAndRules = true;

        Builder() {
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

        public Builder theme(Theme theme) {
            this.theme = requireNonNull(theme);
            return this;
        }

        public Builder removeUriPrefix(String prefix) {
            this.uriFormatter = removePrefix(requireNonNull(prefix));
            return this;
        }

        public Builder includeFeatureAndRules(boolean include) {
            this.includeFeatureAndRules = include;
            return this;
        }

        public MessagesToPrettyWriter build(OutputStream out) {
            requireNonNull(out);
            return new MessagesToPrettyWriter(out, theme, uriFormatter, includeFeatureAndRules);
        }
    }

}
