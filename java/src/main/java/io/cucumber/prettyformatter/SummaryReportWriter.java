package io.cucumber.prettyformatter;

import io.cucumber.messages.Convertor;
import io.cucumber.messages.types.Exception;
import io.cucumber.messages.types.Hook;
import io.cucumber.messages.types.HookType;
import io.cucumber.messages.types.Location;
import io.cucumber.messages.types.Pickle;
import io.cucumber.messages.types.PickleStep;
import io.cucumber.messages.types.Snippet;
import io.cucumber.messages.types.Step;
import io.cucumber.messages.types.StepDefinition;
import io.cucumber.messages.types.Suggestion;
import io.cucumber.messages.types.TestCaseFinished;
import io.cucumber.messages.types.TestCaseStarted;
import io.cucumber.messages.types.TestRunFinished;
import io.cucumber.messages.types.TestRunHookFinished;
import io.cucumber.messages.types.TestStep;
import io.cucumber.messages.types.TestStepFinished;
import io.cucumber.messages.types.TestStepResult;
import io.cucumber.messages.types.TestStepResultStatus;
import io.cucumber.messages.types.UndefinedParameterType;
import io.cucumber.query.Query;
import io.cucumber.query.Repository;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.cucumber.messages.types.TestStepResultStatus.FAILED;
import static io.cucumber.messages.types.TestStepResultStatus.PASSED;
import static io.cucumber.messages.types.TestStepResultStatus.SKIPPED;
import static io.cucumber.messages.types.TestStepResultStatus.UNDEFINED;
import static io.cucumber.prettyformatter.Theme.Element.LOCATION;
import static io.cucumber.prettyformatter.Theme.Element.STEP;
import static io.cucumber.prettyformatter.Theme.Element.STEP_KEYWORD;
import static java.util.Collections.emptyList;
import static java.util.Locale.ROOT;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

final class SummaryReportWriter implements AutoCloseable {

    private final Theme theme;
    private final Function<String, String> uriFormatter;
    private final SourceReferenceFormatter sourceReferenceFormatter;
    private final StepTextFormatter stepTextFormatter;
    private final Set<MessagesToSummaryWriter.SummaryFeature> features;
    private final Query query;
    private final PrintWriter out;

    SummaryReportWriter(
            OutputStream out,
            Theme theme,
            Function<String, String> uriFormatter,
            Set<MessagesToSummaryWriter.SummaryFeature> features,
            Repository data
    ) {
        this.theme = requireNonNull(theme);
        this.out = createPrintWriter(requireNonNull(out));
        this.uriFormatter = requireNonNull(uriFormatter);
        this.sourceReferenceFormatter = new SourceReferenceFormatter(uriFormatter);
        this.stepTextFormatter = new StepTextFormatter();
        this.features = requireNonNull(features);
        this.query = new Query(requireNonNull(data));
    }

    private static PrintWriter createPrintWriter(OutputStream out) {
        return new PrintWriter(
                new OutputStreamWriter(
                        requireNonNull(out),
                        StandardCharsets.UTF_8
                )
        );
    }

    @Override
    public void close() {
        out.close();
    }

    public void printSummary() {
        printNonPassingScenarios();
        printUnknownParameterTypes();
        printNonPassingGlobalHooks();
        printNonPassingTestRun();
        printStats();
        // Print snippets at the end to make copy-pasting easier
        printSnippets();
    }

    private void printStats() {
        out.println();
        printTestRunCount();
        printGlobalHookCount();
        printScenarioCounts();
        printStepCounts();
        printDurations();
    }

    private void printNonPassingGlobalHooks() {
        Map<TestStepResultStatus, List<TestRunHookFinished>> testRunHookFinishedByStatus = query.findAllTestRunHookFinished()
                .stream()
                .collect(groupingBy(SummaryReportWriter::getTestStepResultStatusBy));

        EnumSet<TestStepResultStatus> excluded = EnumSet.of(PASSED, SKIPPED);
        for (TestStepResultStatus status : EnumSet.complementOf(excluded)) {
            printFinishedItemByStatus(
                    "hooks",
                    testRunHookFinishedByStatus,
                    status,
                    this::formatHookLineTo,
                    this::printTestRunHookException
            );
        }
    }

    private void printTestRunHookException(TestRunHookFinished testRunHookFinished, TestStepResultStatus status) {
        TestStepResult result = testRunHookFinished.getResult();
        ExceptionFormatter formatter = new ExceptionFormatter(7, theme, status);
        result.getException()
                .flatMap(formatter::format)
                .ifPresent(out::print);
    }

    private void printNonPassingScenarios() {
        Map<TestStepResultStatus, List<TestCaseFinished>> testCaseFinishedByStatus = findAllTestCasesFinishedInCanonicalOrder()
                .collect(groupingBy(this::getTestStepResultStatusBy));

        EnumSet<TestStepResultStatus> excluded = EnumSet.of(PASSED, SKIPPED);
        for (TestStepResultStatus status : EnumSet.complementOf(excluded)) {
            printFinishedItemByStatus(
                    "scenarios",
                    testCaseFinishedByStatus,
                    status,
                    this::formatScenarioLineTo,
                    this::printResponsibleStep
            );
        }
    }

    private void printResponsibleStep(TestCaseFinished testCaseFinished, TestStepResultStatus status) {
        query.findTestCaseStartedBy(testCaseFinished)
                .map(query::findTestStepFinishedAndTestStepBy)
                .flatMap(list -> list.stream()
                        .filter((entry) -> entry.getKey().getTestStepResult().getStatus() == status)
                        .findFirst())
                .ifPresent(responsibleStep -> {
                    TestStepFinished testStepFinished = responsibleStep.getKey();
                    TestStep testStep = responsibleStep.getValue();

                    query.findPickleStepBy(testStep)
                            .ifPresent(pickleStep -> {
                                query.findStepBy(pickleStep).ifPresent(step -> {
                                    out.println(formatPickleStep(testStepFinished, testStep, pickleStep, step));
                                    pickleStep.getArgument().ifPresent(pickleStepArgument -> {
                                        pickleStepArgument.getDataTable().ifPresent(pickleTable ->
                                                out.print(new LineBuilder(theme)
                                                        .accept(lineBuilder -> PickleTableFormatter.builder()
                                                                .indentation(9)
                                                                .build()
                                                                .formatTo(pickleTable, lineBuilder))
                                                        .build())
                                        );
                                        pickleStepArgument.getDocString().ifPresent(pickleDocString ->
                                                out.print(new LineBuilder(theme)
                                                        .accept(lineBuilder -> PickleDocStringFormatter.builder()
                                                                .indentation(9)
                                                                .build()
                                                                .formatTo(pickleDocString, lineBuilder))
                                                        .build())
                                        );
                                    });
                                });
                            });

                    query.findHookBy(testStep)
                            .ifPresent(hook -> {
                                out.println(formatHookStep(testStepFinished, hook));
                            });

                    ExceptionFormatter formatter = new ExceptionFormatter(11, theme, status);
                    testStepFinished
                            .getTestStepResult()
                            .getException()
                            .flatMap(formatter::format)
                            .ifPresent(out::print);

                    if (features.contains(MessagesToSummaryWriter.SummaryFeature.INCLUDE_ATTACHMENTS)) {
                        query.findAttachmentsBy(testStepFinished).forEach(attachment ->
                                out.print(new LineBuilder(theme)
                                        .newLine()
                                        .accept(lineBuilder -> AttachmentFormatter.builder()
                                                .indentation(11)
                                                .build()
                                                .formatTo(attachment, lineBuilder))
                                        .build())
                        );
                    }
                });
    }

    private String formatHookStep(TestStepFinished testStepFinished, Hook hook) {
        TestStepResultStatus status = testStepFinished.getTestStepResult().getStatus();
        return new LineBuilder(theme)
                .indent(7)
                .begin(STEP, status)
                .append(STEP_KEYWORD, hook.getType()
                        .map(SummaryReportWriter::formatHookType)
                        .orElse("Unknown"))
                .append(hook.getName()
                        .map(name -> "(" + name + ")")
                        .orElse(""))
                .end(STEP, status)
                .accept(lineBuilder -> formatLocationCommentTo(hook, lineBuilder))
                .build();
    }

    private String formatPickleStep(TestStepFinished testStepFinished, TestStep testStep, PickleStep pickleStep, Step step) {
        TestStepResultStatus status = testStepFinished.getTestStepResult().getStatus();
        return new LineBuilder(theme)
                .indent(7)
                .begin(STEP, status)
                .append(STEP_KEYWORD, step.getKeyword())
                .accept(lineBuilder -> stepTextFormatter.formatTo(testStep, pickleStep, lineBuilder))
                .end(STEP, status)
                .accept(lineBuilder -> formatLocationCommentTo(testStep, lineBuilder))
                .build();
    }

    private Stream<TestCaseFinished> findAllTestCasesFinishedInCanonicalOrder() {
        return query.findAllTestCaseFinished().stream()
                .map(testCaseStarted -> {
                    Optional<Pickle> pickle = query.findPickleBy(testCaseStarted);
                    String uri = pickle.map(Pickle::getUri).orElse(null);
                    Long line = pickle.flatMap(query::findLocationOf).map(Location::getLine).orElse(null);
                    return new OrderableMessage<>(testCaseStarted, uri, line);
                })
                .sorted()
                .map(OrderableMessage::getMessage);
    }

    private void formatScenarioLineTo(TestCaseFinished testCaseFinished, LineBuilder lineBuilder) {
        query.findTestCaseStartedBy(testCaseFinished)
                .ifPresent(testCaseStarted -> query.findPickleBy(testCaseStarted)
                        .ifPresent(pickle -> lineBuilder
                                .append(pickle.getName())
                                .append(formatAttempt(testCaseStarted))
                                .accept(innerLineBuilder -> formatLocationCommentTo(pickle, innerLineBuilder))));
    }

    private void formatHookLineTo(TestRunHookFinished testRunHookFinished, LineBuilder lineBuilder) {
        query.findHookBy(testRunHookFinished)
                .ifPresent(hook -> lineBuilder
                        .append(hook.getType()
                                .map(SummaryReportWriter::formatHookType)
                                .orElse("Unknown"))
                        .accept(innerLineBuilder -> hook.getName()
                                .ifPresent(hookName -> innerLineBuilder
                                        .append("(")
                                        .append(hookName)
                                        .append(")")))
                        .accept(innerLineBuilder -> formatLocationCommentTo(hook, lineBuilder)));
    }

    private static String formatHookType(HookType hookType) {
        switch (hookType) {
            case BEFORE_TEST_RUN:
                return "BeforeAll";
            case AFTER_TEST_RUN:
                return "AfterAll";
            case BEFORE_TEST_CASE:
                return "Before";
            case AFTER_TEST_CASE:
                return "After";
            case BEFORE_TEST_STEP:
                return "BeforeStep";
            case AFTER_TEST_STEP:
                return "AfterStep";
            default:
                return "Unknown";
        }
    }

    private <T> void printFinishedItemByStatus(
            String finishedItemName,
            Map<TestStepResultStatus, List<T>> finishedItemByStatus,
            TestStepResultStatus status,
            BiConsumer<T, LineBuilder> formatFinishedItem,
            BiConsumer<T, TestStepResultStatus> printSupplementaryContent
    ) {
        List<T> items = finishedItemByStatus.getOrDefault(status, emptyList());
        if (items.isEmpty()) {
            return;
        }
        out.println();
        String finishItemByStatusTitle = String.format("%s %s:", firstLetterCapitalizedName(status), finishedItemName);
        out.println(theme.style(STEP, status, finishItemByStatusTitle));
        AtomicInteger index = new AtomicInteger(0);
        for (T finishedItem : items) {
            out.println(new LineBuilder(theme)
                    .append("  ")
                    .append(String.valueOf(index.incrementAndGet()))
                    .append(") ")
                    .accept(lineBuilder -> formatFinishedItem.accept(finishedItem, lineBuilder))
                    .build());
            printSupplementaryContent.accept(finishedItem, status);
        }
    }


    private static String formatAttempt(TestCaseStarted testCaseStarted) {
        Long attempt = testCaseStarted.getAttempt();
        if (attempt == 0) {
            return "";
        }
        return ", after " + (attempt + 1) + " attempts";
    }

    private void formatLocationCommentTo(Pickle pickle, LineBuilder lineBuilder) {
        lineBuilder.append(" ")
                .begin(LOCATION)
                .append("# ")
                .append(uriFormatter.apply(pickle.getUri()))
                .accept(innerLineBuilder -> query.findLocationOf(pickle)
                        .ifPresent(location -> lineBuilder.append(":").append(String.valueOf(location.getLine()))))
                .end(LOCATION);
    }

    private void formatLocationCommentTo(TestStep testStep, LineBuilder lineBuilder) {
        query.findUnambiguousStepDefinitionBy(testStep)
                .map(StepDefinition::getSourceReference)
                .flatMap(sourceReferenceFormatter::format)
                .ifPresent(comment -> lineBuilder
                        .append(" ")
                        .append(LOCATION, "# " + comment));
    }

    private void formatLocationCommentTo(Hook hook, LineBuilder lineBuilder) {
        sourceReferenceFormatter.format(hook.getSourceReference())
                .ifPresent(comment -> {
                    lineBuilder.append(" ")
                            .append(LOCATION, "# " + comment);
                });
    }

    private void printNonPassingTestRun() {
        findTestRunWithException()
                .ifPresent(exception -> {
                    out.println(theme.style(STEP, FAILED, firstLetterCapitalizedName(FAILED) + " test run:"));
                    ExceptionFormatter formatter = new ExceptionFormatter(7, theme, FAILED);
                    formatter.format(exception).ifPresent(out::print);
                });
    }

    private Optional<Exception> findTestRunWithException() {
        return query.findTestRunFinished()
                .filter(testRunFinished -> !testRunFinished.getSuccess())
                .flatMap(TestRunFinished::getException);
    }


    private void printTestRunCount() {
        findTestRunWithException()
                // Only print stats if the test run failed with exception, avoid clutter
                .map(exception -> {
                    String subCounts = theme.style(STEP, FAILED, "1 failed");
                    return "1 test run (" + subCounts + ")";
                })
                .ifPresent(out::println);
    }

    private void printGlobalHookCount() {
        List<TestRunHookFinished> testRunHooksFinished = query.findAllTestRunHookFinished();
        if (testRunHooksFinished.isEmpty()) {
            return;
        }

        out.println(formatSubCounts(
                "hooks",
                testRunHooksFinished,
                countTestStepResultStatusByTestRunHookFinished()));

    }

    private void printScenarioCounts() {
        out.println(formatSubCounts(
                "scenarios",
                query.findAllTestCaseFinished(),
                countTestStepResultStatusByTestCaseFinished()));
    }

    private void printStepCounts() {
        out.println(formatSubCounts(
                "steps",
                // findAllTestCaseFinished excludes non-final test cases
                // This ensures findTestStepsFinishedBy does not include retried steps
                query.findAllTestCaseFinished().stream()
                        .map(query::findTestStepsFinishedBy)
                        .flatMap(Collection::stream)
                        .collect(toList()),
                countTestStepResultStatusByTestStepFinished()));
    }

    private <T> String formatSubCounts(
            String itemName,
            Collection<T> finishedItems,
            Collector<T, ?, Map<TestStepResultStatus, Long>> countTestStepResultStatusByItem
    ) {
        String countAndName = finishedItems.size() + " " + itemName;
        StringJoiner joiner = new StringJoiner(", ", countAndName + " (", ")");
        joiner.setEmptyValue(countAndName);
        Map<TestStepResultStatus, Long> subCounts = finishedItems.stream()
                .collect(countTestStepResultStatusByItem);
        for (TestStepResultStatus status : TestStepResultStatus.values()) {
            long count = subCounts.getOrDefault(status, 0L);
            if (count != 0) {
                joiner.add(theme.style(STEP, status, count + " " + status.name().toLowerCase(ROOT)));
            }
        }
        return joiner.toString();
    }

    private void printDurations() {
        query.findTestRunDuration()
                .map(testRunDuration -> String.format("%s (%s executing your code)", formatDuration(testRunDuration), formatDuration(getExecutionDuration())))
                .ifPresent(out::println);
    }

    private Duration getExecutionDuration() {
        Stream<Duration> durationsFromHooks = query.findAllTestRunHookFinished()
                .stream()
                .map(hookFinished -> hookFinished.getResult().getDuration())
                .map(Convertor::toDuration);
        Stream<Duration> durationsFromSteps = query.findAllTestStepFinished()
                .stream()
                .map(hookFinished -> hookFinished.getTestStepResult().getDuration())
                .map(Convertor::toDuration);
        return Stream.concat(durationsFromHooks, durationsFromSteps)
                .reduce(Duration.ZERO, Duration::plus);
    }

    private static String formatDuration(Duration duration) {
        long minutes = duration.toMinutes();
        long seconds = duration.minusMinutes(minutes).getSeconds();
        long milliseconds = NANOSECONDS.toMillis(duration.getNano());
        return String.format("%sm %s.%ss", minutes, seconds, milliseconds);
    }

    private void printUnknownParameterTypes() {
        List<UndefinedParameterType> undefinedParameterTypes = this.query.findAllUndefinedParameterTypes();
        if (undefinedParameterTypes.isEmpty()) {
            return;
        }
        out.println();
        out.println(theme.style(STEP, UNDEFINED, "These parameters are missing a parameter type definition:"));
        int index = 0;
        for (UndefinedParameterType undefinedParameterType : undefinedParameterTypes) {
            String name = undefinedParameterType.getName();
            String expression = undefinedParameterType.getExpression();
            out.println(String.format("  %s) '%s' in '%s'", ++index, name, expression));
        }
    }

    private void printSnippets() {
        Set<Snippet> snippets = findAllTestCasesFinishedInCanonicalOrder()
                .map(query::findPickleBy)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(query::findSuggestionsBy)
                .flatMap(Collection::stream)
                .map(Suggestion::getSnippets)
                .flatMap(Collection::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (snippets.isEmpty()) {
            return;
        }

        out.println();
        out.println("You can implement missing steps with the snippets below:");
        out.println();
        // Don't include any other formatting to make copy-pasting easier.
        for (Snippet snippet : snippets) {
            out.println(snippet.getCode());
            out.println();
        }
    }

    private Collector<TestRunHookFinished, ?, Map<TestStepResultStatus, Long>> countTestStepResultStatusByTestRunHookFinished() {
        return groupingBy(SummaryReportWriter::getTestStepResultStatusBy, counting());
    }

    private static TestStepResultStatus getTestStepResultStatusBy(TestRunHookFinished testRunHookFinished) {
        return testRunHookFinished.getResult().getStatus();
    }

    private Collector<TestCaseFinished, ?, Map<TestStepResultStatus, Long>> countTestStepResultStatusByTestCaseFinished() {
        return groupingBy(this::getTestStepResultStatusBy, counting());
    }

    private TestStepResultStatus getTestStepResultStatusBy(TestCaseFinished testCaseFinished) {
        return query.findMostSevereTestStepResultBy(testCaseFinished)
                .map(TestStepResult::getStatus)
                // By definition
                .orElse(PASSED);
    }

    private static Collector<TestStepFinished, ?, Map<TestStepResultStatus, Long>> countTestStepResultStatusByTestStepFinished() {
        return groupingBy(SummaryReportWriter::getTestStepResultStatusBy, counting());
    }

    private static TestStepResultStatus getTestStepResultStatusBy(TestStepFinished testStepFinished) {
        return testStepFinished.getTestStepResult().getStatus();
    }

    private String firstLetterCapitalizedName(TestStepResultStatus status) {
        String name = status.name();
        return name.charAt(0) + name.substring(1).toLowerCase(ROOT);
    }
}
