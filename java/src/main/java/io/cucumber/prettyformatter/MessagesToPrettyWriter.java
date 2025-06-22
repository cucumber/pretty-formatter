package io.cucumber.prettyformatter;

import io.cucumber.messages.types.Attachment;
import io.cucumber.messages.types.Envelope;
import io.cucumber.messages.types.Exception;
import io.cucumber.messages.types.Group;
import io.cucumber.messages.types.JavaMethod;
import io.cucumber.messages.types.Location;
import io.cucumber.messages.types.Pickle;
import io.cucumber.messages.types.PickleStep;
import io.cucumber.messages.types.PickleTableCell;
import io.cucumber.messages.types.PickleTag;
import io.cucumber.messages.types.Scenario;
import io.cucumber.messages.types.SourceReference;
import io.cucumber.messages.types.Step;
import io.cucumber.messages.types.StepDefinition;
import io.cucumber.messages.types.StepMatchArgument;
import io.cucumber.messages.types.StepMatchArgumentsList;
import io.cucumber.messages.types.TestCase;
import io.cucumber.messages.types.TestCaseStarted;
import io.cucumber.messages.types.TestStep;
import io.cucumber.messages.types.TestStepFinished;
import io.cucumber.messages.types.TestStepResultStatus;
import io.cucumber.query.Lineage;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static io.cucumber.messages.types.TestStepResultStatus.FAILED;
import static java.util.Locale.ROOT;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * Writes a pretty report of the scenario execution as it happens.
 */
class MessagesToPrettyWriter implements AutoCloseable {

    static final String SCENARIO_INDENT = "";
    static final String STEP_INDENT = SCENARIO_INDENT + "  ";
    private static final String STEP_SCENARIO_INDENT = STEP_INDENT + "  ";
    private static final String STACK_TRACE_INDENT = STEP_SCENARIO_INDENT + "  ";

    private final Formats formats;
    private final PrintWriter writer;
    private final PrettyReportData data = new PrettyReportData();
    private boolean streamClosed = false;

    public MessagesToPrettyWriter(OutputStream out) {
        this(out, Formats.ansi());
    }

    public MessagesToPrettyWriter(OutputStream out, Formats formats) {
        requireNonNull(out);
        this.formats = requireNonNull(formats);
        this.writer = new PrintWriter(
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

    private void handleTestCaseStarted(io.cucumber.messages.types.TestCaseStarted event) {
        writer.println();
        printTags(event);
        printScenarioDefinition(event);
        writer.flush();
    }

    private void handleTestStepFinished(io.cucumber.messages.types.TestStepFinished event) {
        printStep(event);
        printError(event);
        writer.flush();
    }

    private void handleAttachment(Attachment attachment) {
        writer.println();
        switch (attachment.getContentEncoding()) {
            case BASE64:
                printEmbedding(attachment);
                break;
            case IDENTITY:
                printText(attachment);
                break;
        }
        writer.println();
        writer.flush();
    }

    private void handleTestRunFinished(io.cucumber.messages.types.TestRunFinished event) {
        printError(event);
        writer.close();
    }

    private void printTags(io.cucumber.messages.types.TestCaseStarted event) {
        data.query.findPickleBy(event)
                .map(Pickle::getTags)
                .filter(pickleTags -> !pickleTags.isEmpty())
                .map(pickleTags -> pickleTags.stream()
                        .map(PickleTag::getName)
                        .collect(joining(" ")))
                .ifPresent(tags -> writer.println(SCENARIO_INDENT + tags));
    }

    private void printScenarioDefinition(TestCaseStarted event) {
        data.query.findTestCaseBy(event).ifPresent(testCase -> {
            data.query.findPickleBy(testCase).ifPresent(pickle -> {
                data.query.findLineageBy(pickle)
                        .flatMap(Lineage::scenario)
                        .ifPresent(scenario -> {
                            printScenarioDefinition(testCase, pickle, scenario);
                        });
            });
        });
    }

    private void printScenarioDefinition(TestCase testCase, Pickle pickle, Scenario scenario) {
        String definitionText = SCENARIO_INDENT + scenario.getKeyword() + ": " + pickle.getName();
        String locationIndent = data.formatLocationIndent(testCase, definitionText);
        String pathWithLine = data.formatPathWithLocation(pickle);
        writer.println(definitionText + locationIndent + formatLocation(pathWithLine));
    }

    private void printStep(io.cucumber.messages.types.TestStepFinished event) {
        data.query.findTestStepBy(event)
                .ifPresent(testStep -> {
                    data.query.findPickleStepBy(testStep).ifPresent(pickleStep -> {
                        data.query.findStepBy(pickleStep).ifPresent(step -> {
                            printStep(event, testStep, pickleStep, step);
                            printArgument(pickleStep);
                        });
                    });
                });
    }

    private void printStep(TestStepFinished event, TestStep testStep, PickleStep pickleStep, Step step) {
        String keyword = step.getKeyword();
        String stepText = pickleStep.getText();
        // TODO: Use proper enum map.
        TestStepResultStatus status = event.getTestStepResult().getStatus();
        List<StepMatchArgument> stepMatchArgumentsLists = testStep.getStepMatchArgumentsLists()
                .map(stepMatchArgumentsLists1 -> stepMatchArgumentsLists1.stream()
                        .map(StepMatchArgumentsList::getStepMatchArguments).flatMap(Collection::stream)
                        .collect(toList()))
                .orElseGet(Collections::emptyList);// TODO: Create separate _arg
        // map

        String formattedStepText = STEP_INDENT + formatStepText(keyword, stepText, formats.status(status), stepMatchArgumentsLists);
        String locationComment = formatLocationComment(event, testStep, keyword, stepText);
        writer.println(formattedStepText + locationComment);
    }

    private void printArgument(PickleStep pickleStep) {
        pickleStep.getArgument().ifPresent(pickleStepArgument -> {

            pickleStepArgument.getDataTable().ifPresent(pickleTable -> {
                List<List<String>> cells = pickleTable.getRows().stream()
                        .map(pickleTableRow -> pickleTableRow.getCells().stream().map(PickleTableCell::getValue)
                                .collect(toList()))
                        .collect(toList());
                DataTableFormatter tableFormatter = DataTableFormatter.builder()
                        .prefixRow(STEP_SCENARIO_INDENT)
                        .escapeDelimiters(false)
                        .build();
                try {
                    tableFormatter.formatTo(cells, writer);
                } catch (IOException e) {
                    // TODO:
                    throw new RuntimeException(e);
                }
            });
            pickleStepArgument.getDocString().ifPresent(pickleDocString -> {
                DocStringFormatter docStringFormatter = DocStringFormatter
                        .builder()
                        .indentation(STEP_SCENARIO_INDENT)
                        .build();
                try {
                    docStringFormatter.formatTo(writer, pickleDocString.getMediaType().orElse(""), pickleDocString.getContent());
                } catch (IOException e) {
                    // TODO:
                    throw new RuntimeException(e);
                }
            });
        });
    }

    private String formatLocationComment(
            TestStepFinished event, TestStep testStep, String keyword, String stepText
    ) {
        return testStep.getStepDefinitionIds()
                .filter(ids -> !ids.isEmpty())
                .map(ids -> {
                    // TODO: Disambiguite?
                    String id = ids.get(0);
                    StepDefinition stepDefinition = data.stepDefinitionsById.get(id);
                    return stepDefinition.getSourceReference();
                })
                .flatMap(MessagesToPrettyWriter::formatSourceReference)
                .map(codeLocation -> data.query.findTestCaseBy(event).map(testCase -> {
                    String prefix = formatPlainStep(keyword, stepText);
                    String locationIndent = data.formatLocationIndent(testCase, prefix);
                    return locationIndent + formatLocation(codeLocation);

                }).orElse("")).orElse("");

    }

    private static Optional<String> formatSourceReference(SourceReference sourceReference) {
        // TODO: can we do this lazy/functional?
        if (sourceReference.getJavaMethod().isPresent()) {
            return formatJavaMethod(sourceReference);
        }
        if (sourceReference.getJavaStackTraceElement().isPresent()) {
            return formatStackTraceElement(sourceReference);
        }
        if (sourceReference.getUri().isPresent()) {
            return formatUri(sourceReference);
        }
        return Optional.empty();
    }

    private static Optional<String> formatUri(SourceReference sourceReference) {
        return sourceReference.getUri()
                .map(uri -> uri + sourceReference.getLocation()
                        .map(location -> ":" + location.getLine())
                        .orElse(""));
    }

    private static Optional<String> formatJavaMethod(SourceReference sourceReference) {
        return sourceReference.getJavaMethod()
                .map(MessagesToPrettyWriter::formatJavaMethod);
    }

    private static Optional<String> formatStackTraceElement(SourceReference sourceReference) {
        String location = sourceReference.getLocation().map(Location::getLine).map(line -> ":" + line).orElse("");
        return sourceReference.getJavaStackTraceElement()
                .map(javaStackTraceElement -> String.format("%s.%s(%s%s)",
                        javaStackTraceElement.getClassName(),
                        javaStackTraceElement.getMethodName(),
                        javaStackTraceElement.getFileName(),
                        location));
    }

    private static String formatJavaMethod(JavaMethod javaMethod) {
        return javaMethod.getClassName() + "." + javaMethod.getMethodName() + "("
                + javaMethod.getMethodParameterTypes().stream().collect(joining(",")) + ")";
    }

    private void printError(TestStepFinished event) {
        event.getTestStepResult()
                .getException()
                .ifPresent(exception -> {
                    TestStepResultStatus name = event.getTestStepResult().getStatus();
                    printError(STACK_TRACE_INDENT, exception, formats.status(name));
                });
    }

    private void printError(io.cucumber.messages.types.TestRunFinished event) {
        event.getException()
                .ifPresent(exception -> printError(SCENARIO_INDENT, exception, formats.status(FAILED)));
    }

    private void printError(String scenarioIndent, Exception exception, Format format) {
        String text = exception.getStackTrace().orElseGet(() -> exception.getMessage().orElse(""));
        // TODO: Java 12+ use String.indent
        String indented = text.replaceAll("(\r\n|\r|\n)", "$1" + scenarioIndent).trim();
        writer.println(scenarioIndent + format.color(indented));
    }

    private void printText(Attachment event) {
        // Prevent interleaving when multiple threads write to System.out
        StringBuilder builder = new StringBuilder();
        try (BufferedReader lines = new BufferedReader(new StringReader(event.getBody()))) {
            String line;
            while ((line = lines.readLine()) != null) {
                builder.append(STEP_SCENARIO_INDENT)
                        .append(line)
                        // Add system line separator - \n won't do it!
                        .append(System.lineSeparator());
            }
        } catch (IOException e) {
            // TODO:
            throw new RuntimeException(e);
        }
        writer.append(builder);
    }

    private void printEmbedding(Attachment event) {
        int bytes = (event.getBody().length() / 4) * 3;
        String line;
        if (event.getFileName().isPresent()) {
            line = String.format("Embedding %s [%s %d bytes]", event.getFileName().get(), event.getMediaType(), bytes);
        } else {
            line = String.format("Embedding [%s %d bytes]", event.getMediaType(), bytes);
        }
        writer.println(STEP_SCENARIO_INDENT + line);
    }

    private String formatPlainStep(String keyword, String stepText) {
        return STEP_INDENT + keyword + stepText;
    }

    static URI relativize(String uri) {
        return relativize(URI.create(uri));
    }

    static URI relativize(URI uri) {
        if (!"file".equals(uri.getScheme())) {
            return uri;
        }
        if (!uri.isAbsolute()) {
            return uri;
        }

        try {
            URI root = new File("").toURI();
            URI relative = root.relativize(uri);
            // Scheme is lost by relativize
            return new URI("file", relative.getSchemeSpecificPart(), relative.getFragment());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    private String formatLocation(String location) {
        return formats.comment().color("# " + location);
    }

    String formatStepText(
            String keyword, String stepText, Format textFormat, List<StepMatchArgument> arguments
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
                // a nested argument starts before the enclosing argument ends;
                // ignore it when formatting
                if (argumentOffset < beginIndex) {
                    continue;
                }
                String text = stepText.substring(beginIndex, argumentOffset);
                result.append(text);
                int argumentEndIndex = argumentOffset + value.get().length();
                result.append(textFormat.bold(stepText.substring(argumentOffset, argumentEndIndex)));
                beginIndex = argumentEndIndex;
            }
        }
        if (beginIndex != stepText.length()) {
            String text = stepText.substring(beginIndex);
            result.append(text);
        }
        return textFormat.color(result.toString());
    }

    /**
     * Closes the stream, flushing it first. Once closed further write()
     * invocations will cause an IOException to be thrown. Closing a closed
     * stream has no effect.
     *
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
