package io.cucumber.prettyformatter;

import io.cucumber.messages.types.Attachment;
import io.cucumber.messages.types.Feature;
import io.cucumber.messages.types.Group;
import io.cucumber.messages.types.Pickle;
import io.cucumber.messages.types.PickleStep;
import io.cucumber.messages.types.PickleTag;
import io.cucumber.messages.types.Rule;
import io.cucumber.messages.types.Scenario;
import io.cucumber.messages.types.Step;
import io.cucumber.messages.types.StepMatchArgument;
import io.cucumber.messages.types.TestCaseStarted;
import io.cucumber.messages.types.TestRunFinished;
import io.cucumber.messages.types.TestStep;
import io.cucumber.messages.types.TestStepFinished;
import io.cucumber.messages.types.TestStepResult;
import io.cucumber.messages.types.TestStepResultStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static io.cucumber.messages.types.TestStepResultStatus.FAILED;
import static io.cucumber.prettyformatter.MessagesToPrettyWriter.PrettyFeature.INCLUDE_ATTACHMENTS;
import static io.cucumber.prettyformatter.MessagesToPrettyWriter.PrettyFeature.INCLUDE_FEATURE_LINE;
import static io.cucumber.prettyformatter.MessagesToPrettyWriter.PrettyFeature.INCLUDE_RULE_LINE;
import static io.cucumber.prettyformatter.MessagesToPrettyWriter.PrettyFeature.USE_STATUS_ICON;
import static io.cucumber.prettyformatter.Theme.Element.ATTACHMENT;
import static io.cucumber.prettyformatter.Theme.Element.FEATURE;
import static io.cucumber.prettyformatter.Theme.Element.FEATURE_KEYWORD;
import static io.cucumber.prettyformatter.Theme.Element.FEATURE_NAME;
import static io.cucumber.prettyformatter.Theme.Element.LOCATION;
import static io.cucumber.prettyformatter.Theme.Element.RULE;
import static io.cucumber.prettyformatter.Theme.Element.RULE_KEYWORD;
import static io.cucumber.prettyformatter.Theme.Element.RULE_NAME;
import static io.cucumber.prettyformatter.Theme.Element.SCENARIO;
import static io.cucumber.prettyformatter.Theme.Element.SCENARIO_KEYWORD;
import static io.cucumber.prettyformatter.Theme.Element.SCENARIO_NAME;
import static io.cucumber.prettyformatter.Theme.Element.STATUS_ICON;
import static io.cucumber.prettyformatter.Theme.Element.STEP;
import static io.cucumber.prettyformatter.Theme.Element.STEP_ARGUMENT;
import static io.cucumber.prettyformatter.Theme.Element.STEP_KEYWORD;
import static io.cucumber.prettyformatter.Theme.Element.STEP_TEXT;
import static io.cucumber.prettyformatter.Theme.Element.TAG;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

final class PrettyReportWriter implements AutoCloseable {

    private final Theme theme;
    private final SourceReferenceFormatter sourceReferenceFormatter;
    private final Function<String, String> uriFormatter;
    private final PrintWriter writer;
    private final Set<MessagesToPrettyWriter.PrettyFeature> features;
    private final PrettyReportData data;

    PrettyReportWriter(
            OutputStream out,
            Theme theme,
            Function<String, String> uriFormatter,
            Set<MessagesToPrettyWriter.PrettyFeature> features, 
            PrettyReportData data

    ) {
        this.theme = requireNonNull(theme);
        this.writer = createPrintWriter(requireNonNull(out));
        this.uriFormatter = requireNonNull(uriFormatter);
        this.features = features;
        this.data = data;
        this.sourceReferenceFormatter = new SourceReferenceFormatter(uriFormatter);
    }

    private static PrintWriter createPrintWriter(OutputStream out) {
        return new PrintWriter(
                new OutputStreamWriter(
                        requireNonNull(out),
                        StandardCharsets.UTF_8
                )
        );
    }

    void handleTestCaseStarted(TestCaseStarted event) {
        data.findLineageBy(event).ifPresent(lineage -> {
            if (features.contains(INCLUDE_FEATURE_LINE)) {
                lineage.feature().ifPresent(this::printFeature);
            }
            if (features.contains(INCLUDE_RULE_LINE)) {
                lineage.rule().ifPresent(this::printRule);
            }
        });

        writer.println();
        printTags(event);
        printScenarioDefinition(event);
        writer.flush();
    }

    private void printFeature(Feature feature) {
        data.ifNotSeenBefore(feature, () -> {
            writer.println();
            writer.println(new LineBuilder(theme)
                    .begin(FEATURE)
                    .title(FEATURE_KEYWORD, feature.getKeyword(), FEATURE_NAME, feature.getName())
                    .end(FEATURE)
                    .build());
        });
    }

    private void printRule(Rule rule) {
        data.ifNotSeenBefore(rule, () ->
                writer.println(new LineBuilder(theme)
                        .newLine()
                        .indent(data.getAfterFeatureIndent())
                        .begin(RULE)
                        .title(RULE_KEYWORD, rule.getKeyword(), RULE_NAME, rule.getName())
                        .end(RULE)
                        .build()));
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
                .begin(SCENARIO)
                .title(SCENARIO_KEYWORD, scenario.getKeyword(), SCENARIO_NAME, pickle.getName())
                .end(SCENARIO)
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

    void handleTestStepFinished(TestStepFinished event) {
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
                .accept(lineBuilder -> formatStatusIcon(lineBuilder, status))
                .begin(STEP, status)
                .append(STEP_KEYWORD, step.getKeyword())
                .accept(lineBuilder -> formatStepText(lineBuilder, testStep, pickleStep))
                .end(STEP, status)
                .accept(lineBuilder -> formatLocation(testStep)
                        .ifPresent(location -> lineBuilder
                                .addPaddingUpTo(data.getCommentStartAtIndexBy(event))
                                .append(LOCATION, "# " + location)
                        )
                )
                .build();
    }

    private void formatStatusIcon(LineBuilder lineBuilder, TestStepResultStatus status) {
        if (!features.contains(USE_STATUS_ICON)) {
            return;
        }
        lineBuilder
                .begin(STATUS_ICON, status)
                .statusIcon(theme.statusIcon(status))
                .end(STATUS_ICON, status)
                .append(" ");
    }

    private void formatStepText(LineBuilder line, TestStep testStep, PickleStep pickleStep) {
        formatStepText(line, pickleStep.getText(), getStepMatchArguments(testStep));
    }

    private List<StepMatchArgument> getStepMatchArguments(TestStep testStep) {
        List<StepMatchArgument> stepMatchArguments = new ArrayList<>();
        testStep.getStepMatchArgumentsLists()
                .filter(stepMatchArgumentsList -> stepMatchArgumentsList.size() == 1)
                .orElse(emptyList())
                .forEach(list -> stepMatchArguments.addAll(list.getStepMatchArguments()));
        return stepMatchArguments;
    }

    void formatStepText(LineBuilder lineBuilder, String stepText, List<StepMatchArgument> arguments) {
        int currentIndex = 0;
        for (StepMatchArgument argument : arguments) {
            Group group = argument.getGroup();
            // Ignore absent values, or groups without a start
            if (group.getValue().isPresent() && group.getStart().isPresent()) {
                String groupValue = group.getValue().get();
                // TODO: Messages are silly
                int groupStart = (int) (long) group.getStart().get();
                String text = stepText.substring(currentIndex, groupStart);
                currentIndex = groupStart + groupValue.length();
                lineBuilder.append(STEP_TEXT, text)
                        .append(STEP_ARGUMENT, groupValue);
            }
        }
        if (currentIndex != stepText.length()) {
            String remainder = stepText.substring(currentIndex);
            lineBuilder.append(STEP_TEXT, remainder);
        }
    }

    private Optional<String> formatLocation(TestStep testStep) {
        return data.findSourceReferenceBy(testStep)
                .flatMap(sourceReferenceFormatter::format);
    }


    private void printException(TestStepFinished event) {
        int indent = data.getStackTraceIndentBy(event);
        TestStepResult testStepResult = event.getTestStepResult();
        TestStepResultStatus status = testStepResult.getStatus();
        ExceptionFormatter formatter = new ExceptionFormatter(indent, theme, status);
        testStepResult.getException()
                .map(formatter::format)
                // Fallback
                .orElseGet(() -> testStepResult.getMessage().map(formatter::format))
                .ifPresent(writer::print);
    }

    void handleAttachment(Attachment attachment) {
        if (!features.contains(INCLUDE_ATTACHMENTS)) {
            return;
        }
        writer.println();
        switch (attachment.getContentEncoding()) {
            case BASE64 -> writer.println(formatBase64Attachment(attachment));
            case IDENTITY -> writer.print(formatTextAttachment(attachment));
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
        int indent = data.getAttachmentIndentBy(event);
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

    void handleTestRunFinished(TestRunFinished event) {
        event.getException().ifPresent(exception -> {
            ExceptionFormatter formatter = new ExceptionFormatter(0, theme, FAILED);
            formatter.format(exception).ifPresent(writer::print);
        });
    }

    @Override
    public void close() {
        writer.close();
    }
}
