package io.cucumber.prettyformatter;

import io.cucumber.messages.types.Exception;
import io.cucumber.messages.types.Hook;
import io.cucumber.messages.types.HookType;
import io.cucumber.messages.types.Location;
import io.cucumber.messages.types.Pickle;
import io.cucumber.messages.types.Snippet;
import io.cucumber.messages.types.Suggestion;
import io.cucumber.messages.types.TestCaseFinished;
import io.cucumber.messages.types.TestCaseStarted;
import io.cucumber.messages.types.TestRunFinished;
import io.cucumber.messages.types.TestRunHookFinished;
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
    private final Query query;
    private final PrintWriter out;
    private final List<UndefinedParameterType> undefinedParameterTypes;

    SummaryReportWriter(
            OutputStream out,
            Theme theme,
            Function<String, String> uriFormatter,
            Repository data,
            List<UndefinedParameterType> undefinedParameterTypes
    ) {
        this.theme = requireNonNull(theme);
        this.out = createPrintWriter(requireNonNull(out));
        this.uriFormatter = requireNonNull(uriFormatter);
        this.sourceReferenceFormatter = new SourceReferenceFormatter(uriFormatter);
        this.query = new Query(requireNonNull(data));
        this.undefinedParameterTypes = requireNonNull(undefinedParameterTypes);
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
        printDuration();
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
                    this::formatHookLine,
                    testRunHookFinished -> Optional.of(testRunHookFinished.getResult())

            );
        }
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
                    this::formatScenarioLine,
                    query::findMostSevereTestStepResultBy
            );
        }
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

    private Optional<String> formatScenarioLine(TestCaseFinished testCaseFinished) {
        return query.findTestCaseStartedBy(testCaseFinished)
                .flatMap(testCaseStarted -> query.findPickleBy(testCaseStarted)
                        .map(pickle -> {
                            String name = pickle.getName();
                            String attempt = formatAttempt(testCaseStarted);
                            String location = formatLocationComment(pickle);
                            return String.format("%s%s%s", name, attempt, location);
                        }));
    }

    private Optional<String> formatHookLine(TestRunHookFinished testRunHookFinished) {
        return query.findHookBy(testRunHookFinished)
                .map(hook -> {
                    String hookTypeName = hook.getType()
                            .map(SummaryReportWriter::formatGlobalHookName)
                            .orElse("Unknown");
                    String hookName = hook.getName()
                            .map(name -> "(" + name + ")")
                            .orElse("");
                    String location = formatLocationComment(hook);
                    return String.format("%s%s%s", hookTypeName, hookName, location);
                });
    }

    private static String formatGlobalHookName(HookType hookType) {
        switch (hookType) {
            case BEFORE_TEST_RUN:
                return "BeforeTestRun";
            case AFTER_TEST_RUN:
                return "AfterTestRun";
            default:
                return "Unknown";
        }
    }

    private <T> void printFinishedItemByStatus(
            String finishedItemName,
            Map<TestStepResultStatus, List<T>> finishedItemByStatus,
            TestStepResultStatus status,
            Function<T, Optional<String>> formatFinishedItem,
            Function<T, Optional<TestStepResult>> getTestStepResult

    ) {
        List<T> items = finishedItemByStatus.getOrDefault(status, emptyList());
        if (items.isEmpty()) {
            return;
        }
        out.println();
        String finishItemByStatusTitle = String.format("%s %s:", firstLetterCapitalizedName(status), finishedItemName);
        out.println(theme.style(STEP, status, finishItemByStatusTitle));
        ExceptionFormatter formatter = new ExceptionFormatter(7, theme, status);
        AtomicInteger index = new AtomicInteger(0);
        for (T testCaseFinished : items) {
            formatFinishedItem.apply(testCaseFinished)
                    .map(line -> String.format("  %d) %s", index.incrementAndGet(), line))
                    .ifPresent(out::println);
            getTestStepResult.apply(testCaseFinished)
                    .flatMap(TestStepResult::getException)
                    .flatMap(formatter::format)
                    .ifPresent(out::println);
        }
    }


    private static String formatAttempt(TestCaseStarted testCaseStarted) {
        Long attempt = testCaseStarted.getAttempt();
        if (attempt == 0) {
            return "";
        }
        return ", after " + (attempt + 1) + " attempts";
    }

    private String formatLocationComment(Pickle pickle) {
        String formattedUri = uriFormatter.apply(pickle.getUri());
        String comment = "# " + formattedUri + query.findLocationOf(pickle)
                .map(Location::getLine)
                .map(line -> ":" + line)
                .orElse("");
        return " " + theme.style(LOCATION, comment);
    }

    private String formatLocationComment(Hook hook) {
        return sourceReferenceFormatter.format(hook.getSourceReference())
                .map(comment -> theme.style(LOCATION, "#" + comment))
                .map(comment -> " " + comment)
                .orElse("");
    }

    private void printNonPassingTestRun() {
        findTestRunWithException()
                .ifPresent(exception -> {
                    out.println(theme.style(STEP, FAILED, firstLetterCapitalizedName(FAILED) + " test run:"));
                    ExceptionFormatter formatter = new ExceptionFormatter(7, theme, FAILED);
                    out.println(formatter.format(exception));
                });
    }

    private Optional<Exception> findTestRunWithException() {
        return query.findTestRunFinished()
                .filter(testRunFinished -> !testRunFinished.getSuccess())
                .flatMap(TestRunFinished::getException);
    }


    private void printTestRunCount() {
        // TODO: No coverage?
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

    private void printDuration() {
        query.findTestRunDuration()
                .map(SummaryReportWriter::formatDuration)
                .ifPresent(out::println);
    }

    private static String formatDuration(Duration duration) {
        long minutes = duration.toMinutes();
        long seconds = duration.minusMinutes(minutes).getSeconds();
        long milliseconds = NANOSECONDS.toMillis(duration.getNano());
        return String.format("%sm %s.%ss", minutes, seconds, milliseconds);
    }

    private void printUnknownParameterTypes() {
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
