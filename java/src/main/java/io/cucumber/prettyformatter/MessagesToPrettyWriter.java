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

    private final Formatter formatter;
    private final Function<String, String> uriFormatter;
    private final PrintWriter writer;
    private final PrettyReportData data = new PrettyReportData();
    private boolean streamClosed = false;

    public MessagesToPrettyWriter(OutputStream out) {
        this(createPrintWriter(out), Formatter.ansi(), Function.identity());
    }

    private MessagesToPrettyWriter(PrintWriter writer, Formatter formatter, Function<String, String> uriFormatter) {
        this.uriFormatter = uriFormatter;
        this.formatter = formatter;
        this.writer = writer;
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
        return s -> {
            if (s.startsWith(prefix)) {
                return s.substring(prefix.length());
            }
            return s;
        };
    }

    public MessagesToPrettyWriter withNoAnsiColors() {
        return new MessagesToPrettyWriter(writer, Formatter.noAnsi(), uriFormatter);
    }

    public MessagesToPrettyWriter withRemovePathPrefix(String prefix) {
        return new MessagesToPrettyWriter(writer, Formatter.ansi(), removePrefix(prefix));
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
        return pickleTags.stream()
                .map(PickleTag::getName)
                .collect(joining(" ", SCENARIO_INDENT, ""));
    }

    private void printScenarioDefinition(TestCaseStarted event) {
        data.findPickleBy(event).ifPresent(pickle ->
                data.findScenarioBy(pickle).ifPresent(scenario ->
                        writer.println(formatScenarioLine(event, pickle, scenario))));
    }

    private String formatScenarioLine(TestCaseStarted event, Pickle pickle, Scenario scenario) {
        String scenarioTitle = scenario.getKeyword() + ": " + pickle.getName();
        int unformattedScenarioTextLength = SCENARIO_INDENT.length() + scenarioTitle.length();
        String locationComment = formatLocation(pickle);
        String locationIndent = locationComment.isEmpty() ? "" : data.getLocationIndentFor(event, unformattedScenarioTextLength);
        return SCENARIO_INDENT + formatter.scenario(scenarioTitle) + locationIndent + locationComment;
    }

    private String formatLocation(Pickle pickle) {
        String path = uriFormatter.apply(pickle.getUri());
        String pathWithLine = data.findLineOf(pickle)
                .map(line -> path + ":" + line)
                .orElse(path);

        return formatLocation(pathWithLine);
    }

    private String formatLocation(String location) {
        return formatter.comment("# " + location);
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
                                        writer.print(pickleTableFormatter.format(pickleTable))
                                );
                                pickleStepArgument.getDocString().ifPresent(pickleDocString ->
                                        writer.print(pickleDocStringFormatter.format(pickleDocString))
                                );
                            });
                        })));
    }

    private String formatStep(TestStepFinished event, TestStep testStep, PickleStep pickleStep, Step step) {
        int unformattedStepTextLength = STEP_INDENT.length() + step.getKeyword().length() + pickleStep.getText().length();
        String locationComment = formatLocation(testStep);
        String locationIndent = locationComment.isEmpty() ? "" : data.getLocationIndentFor(event, unformattedStepTextLength);
        return STEP_INDENT + formatStepText(event, testStep, pickleStep, step) + locationIndent + locationComment;
    }

    private String formatStepText(TestStepFinished event, TestStep testStep, PickleStep pickleStep, Step step) {
        String keyword = step.getKeyword();
        String stepText = pickleStep.getText();
        TestStepResultStatus status = event.getTestStepResult().getStatus();
        List<StepMatchArgument> stepMatchArguments = testStep.getStepMatchArgumentsLists()
                .map(stepMatchArgumentsLists -> stepMatchArgumentsLists.stream()
                        .map(StepMatchArgumentsList::getStepMatchArguments)
                        .flatMap(Collection::stream)
                        .collect(toList())
                )
                .orElseGet(Collections::emptyList);

        return formatStepText(keyword, stepText, status, stepMatchArguments);
    }

    String formatStepText(
            String keyword, String stepText, TestStepResultStatus status, List<StepMatchArgument> arguments
    ) {
        int beginIndex = 0;
        StringBuilder result = new StringBuilder(keyword);
        for (StepMatchArgument argument : arguments) {
            // can be null if the argument is missing.
            Group group = argument.getGroup();
            Optional<String> value = group.getValue();
            if (value.isPresent()) {
                // TODO: Messages are silly
                int argumentOffset = (int) (long) group.getStart().orElse(-1L);
                String text = stepText.substring(beginIndex, argumentOffset);
                result.append(text);
                int argumentEndIndex = argumentOffset + value.get().length();
                result.append(formatter.argument(stepText.substring(argumentOffset, argumentEndIndex)));
                beginIndex = argumentEndIndex;
            }
        }
        if (beginIndex != stepText.length()) {
            String text = stepText.substring(beginIndex);
            result.append(text);
        }
        return formatter.step(status, result.toString());
    }

    private String formatLocation(TestStep testStep) {
        return data.findSourceReferenceBy(testStep)
                .flatMap(this::formatSourceReference)
                .map(location -> formatter.comment("# " + location))
                .orElse("");
    }

    private Optional<String> formatSourceReference(SourceReference sourceReference) {
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
                writer.println(formatBase64Embedding(attachment));
                break;
            case IDENTITY:
                writer.print(formatTextAttachment(attachment));
                break;
        }
        writer.println();
        writer.flush();
    }

    private String formatBase64Embedding(Attachment event) {
        int bytes = (event.getBody().length() / 4) * 3;
        String line;
        if (event.getFileName().isPresent()) {
            line = String.format("Embedding %s [%s %d bytes]", event.getFileName().get(), event.getMediaType(), bytes);
        } else {
            line = String.format("Embedding [%s %d bytes]", event.getMediaType(), bytes);
        }
        return STEP_SCENARIO_INDENT + formatter.output(line);
    }

    private StringBuilder formatTextAttachment(Attachment event) {
        // Prevent interleaving when multiple threads write to System.out
        StringBuilder builder = new StringBuilder();
        try (BufferedReader lines = new BufferedReader(new StringReader(event.getBody()))) {
            String line;
            while ((line = lines.readLine()) != null) {
                builder.append(STEP_SCENARIO_INDENT)
                        .append(formatter.output(line))
                        // Add system line separator - \n won't do it!
                        .append(System.lineSeparator());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return builder;
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
        return scenarioIndent + formatter.error(status, indented);
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
