package io.cucumber.prettyformatter;

import io.cucumber.messages.types.Attachment;
import io.cucumber.messages.types.Envelope;
import io.cucumber.messages.types.Exception;
import io.cucumber.messages.types.Group;
import io.cucumber.messages.types.Location;
import io.cucumber.messages.types.Pickle;
import io.cucumber.messages.types.PickleStep;
import io.cucumber.messages.types.PickleTag;
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
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * Writes a pretty report of the scenario execution as it happens.
 */
public class MessagesToPrettyWriter implements AutoCloseable {

    static final String SCENARIO_INDENT = "";
    static final String STEP_INDENT = SCENARIO_INDENT + "  ";
    private static final String STEP_SCENARIO_INDENT = STEP_INDENT + "  ";
    private static final String STACK_TRACE_INDENT = STEP_SCENARIO_INDENT + "  ";

    private final PickleTableFormatter pickleTableFormatter = PickleTableFormatter.builder()
            .prefixRow(STEP_SCENARIO_INDENT)
            .build();

    private final PickleDocStringFormatter pickleDocStringFormatter = PickleDocStringFormatter.builder()
            .indentation(STEP_SCENARIO_INDENT)
            .build();

    private final Theme theme;
    private final Function<String, String> uriFormatter;
    private final PrintWriter writer;
    private final PrettyReportData data = new PrettyReportData();
    private boolean streamClosed = false;

    public MessagesToPrettyWriter(OutputStream out) {
        this(createPrintWriter(out), Theme.cucumberJvm(), Function.identity());
    }

    public MessagesToPrettyWriter(OutputStream out, Theme theme) {
        this(createPrintWriter(out), theme, Function.identity());
    }

    private MessagesToPrettyWriter(PrintWriter writer, Theme theme, Function<String, String> uriFormatter) {
        this.theme = theme;
        this.writer = writer;
        this.uriFormatter = uriFormatter;
    }

    private static PrintWriter createPrintWriter(OutputStream out) {
        return new PrintWriter(
                new OutputStreamWriter(
                        requireNonNull(out),
                        StandardCharsets.UTF_8
                )
        );
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

    public MessagesToPrettyWriter withNoAnsiColors() {
        // TODO: With something better
        return new MessagesToPrettyWriter(writer, Theme.noColor(), uriFormatter);
    }

    public MessagesToPrettyWriter withRemovePathPrefix(String prefix) {
        // TODO: With something better
        return new MessagesToPrettyWriter(writer, theme, removePrefix(prefix));
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

    private void handleTestCaseStarted(io.cucumber.messages.types.TestCaseStarted event) {
        writer.println();
        printTags(event);
        printScenarioDefinition(event);
        writer.flush();
    }

    private void printTags(io.cucumber.messages.types.TestCaseStarted event) {
        data.findTagsBy(event)
                .map(this::formatTagLine)
                .ifPresent(writer::println);
    }

    private String formatTagLine(List<PickleTag> pickleTags) {
        String tags = pickleTags.stream()
                .map(PickleTag::getName)
                .collect(joining(" "));
        return new LineBuilder(theme)
                .indent(SCENARIO_INDENT)
                .tag(tags)
                .build();
    }

    private void printScenarioDefinition(TestCaseStarted event) {
        data.findPickleBy(event).ifPresent(pickle ->
                data.findScenarioBy(pickle).ifPresent(scenario ->
                        writer.println(formatScenarioLine(event, pickle, scenario))));
    }

    private String formatScenarioLine(TestCaseStarted event, Pickle pickle, Scenario scenario) {
        return new LineBuilder(theme)
                .indent(SCENARIO_INDENT)
                .scenario(scenario.getKeyword(), pickle.getName())
                .addPaddingUpTo(data.getCommentStartAtIndexBy(event))
                .location(formatLocation(pickle))
                .build();
    }

    private String formatLocation(Pickle pickle) {
        String path = uriFormatter.apply(pickle.getUri());
        return data.findLineOf(pickle)
                .map(line -> path + ":" + line)
                .orElse(path);
    }

    private void handleTestStepFinished(io.cucumber.messages.types.TestStepFinished event) {
        printStep(event);
        printException(event);
        writer.flush();
    }

    private void printStep(io.cucumber.messages.types.TestStepFinished event) {
        data.findTestStepBy(event).ifPresent(testStep ->
                data.findPickleStepBy(testStep).ifPresent(pickleStep ->
                        data.findStepBy(pickleStep).ifPresent(step -> {
                            writer.println(formatStep(event, testStep, pickleStep, step));
                            pickleStep.getArgument().ifPresent(pickleStepArgument -> {
                                pickleStepArgument.getDataTable().ifPresent(pickleTable ->
                                        writer.print(new LineBuilder(theme)
                                                .accept(lineBuilder -> pickleTableFormatter.formatTo(pickleTable, lineBuilder))
                                                .build())
                                );
                                pickleStepArgument.getDocString().ifPresent(pickleDocString ->
                                        writer.print(new LineBuilder(theme)
                                                .accept(lineBuilder -> pickleDocStringFormatter.formatTo(pickleDocString, lineBuilder))
                                                .build())
                                );
                            });
                        })));
    }

    private String formatStep(TestStepFinished event, TestStep testStep, PickleStep pickleStep, Step step) {
        return new LineBuilder(theme)
                .indent(STEP_INDENT)
                .beginStep(event.getTestStepResult().getStatus())
                .stepKeyword(step.getKeyword())
                .accept(lineBuilder -> formatStepText(lineBuilder, testStep, pickleStep))
                .endStep(event.getTestStepResult().getStatus())
                .accept(lineBuilder -> formatLocation(testStep)
                        .ifPresent(location -> lineBuilder
                                .addPaddingUpTo(data.getCommentStartAtIndexBy(event))
                                .location(location)))
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
                lineBuilder
                        .stepText(text)
                        .stepArgument(stepText.substring(argumentOffset, argumentEndIndex));
            }
        }
        if (beginIndex != stepText.length()) {
            lineBuilder.stepText(stepText.substring(beginIndex));
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
        event.getTestStepResult().getException().ifPresent(exception ->
                writer.println(formatError(STACK_TRACE_INDENT, exception, event.getTestStepResult().getStatus())));
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
                .indent(STEP_SCENARIO_INDENT)
                .attachment(line)
                .build();
    }

    private String formatTextAttachment(Attachment event) {
        // Prevent interleaving when multiple threads write to System.out
        LineBuilder builder = new LineBuilder(theme);
        try (BufferedReader lines = new BufferedReader(new StringReader(event.getBody()))) {
            String line;
            while ((line = lines.readLine()) != null) {
                builder.indent(STEP_SCENARIO_INDENT)
                        .attachment(line)
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
                writer.println(formatError(SCENARIO_INDENT, exception, FAILED)));
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

}
