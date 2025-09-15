package io.cucumber.prettyformatter;

import io.cucumber.messages.types.Location;
import io.cucumber.messages.types.Pickle;
import io.cucumber.messages.types.Snippet;
import io.cucumber.messages.types.Suggestion;
import io.cucumber.messages.types.TestCaseFinished;
import io.cucumber.messages.types.TestCaseStarted;
import io.cucumber.messages.types.TestStepFinished;
import io.cucumber.messages.types.TestStepResult;
import io.cucumber.messages.types.TestStepResultStatus;
import io.cucumber.query.Query;
import io.cucumber.query.Repository;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
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

import static io.cucumber.prettyformatter.Theme.Element.LOCATION;
import static io.cucumber.prettyformatter.Theme.Element.STATUS_ICON;
import static java.util.Collections.emptyList;
import static java.util.Locale.ROOT;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

final class SummaryReportWriter implements AutoCloseable {

    private final Theme theme;
    private final Function<String, String> uriFormatter;
    private final Query query;
    private final PrintWriter out;

    SummaryReportWriter(
            OutputStream out,
            Theme theme,
            Function<String, String> uriFormatter,
            Repository data

    ) {
        this.theme = requireNonNull(theme);
        this.out = createPrintWriter(requireNonNull(out));
        this.uriFormatter = requireNonNull(uriFormatter);
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
        out.println();
        printNonPassingScenarios();
        printStats();
        printSnippets();
    }

    private void printStats() {
        printScenarioCounts();
        printStepCounts();
        printDuration();
    }

    private void printNonPassingScenarios() {
        // TODO: Print in canonical order?
        Map<TestStepResultStatus, List<TestCaseFinished>> testCaseFinishedByStatus = query.findAllTestCaseFinished()
                .stream()
                .collect(groupingBy(this::getTestStepResultStatusBy));

        printScenarios(testCaseFinishedByStatus, TestStepResultStatus.PENDING);
        printScenarios(testCaseFinishedByStatus, TestStepResultStatus.UNDEFINED);
        printScenarios(testCaseFinishedByStatus, TestStepResultStatus.AMBIGUOUS);
        printScenarios(testCaseFinishedByStatus, TestStepResultStatus.FAILED);
    }

    private void printScenarios(
            Map<TestStepResultStatus, List<TestCaseFinished>> testCaseFinishedByStatus,
            TestStepResultStatus status
    ) {
        List<TestCaseFinished> testCasesFinished = testCaseFinishedByStatus.getOrDefault(status, emptyList());
        if (!testCasesFinished.isEmpty()) {
            out.println(theme.style(STATUS_ICON, status, firstLetterCapitalizedName(status) + " scenarios:"));
        }
        ExceptionFormatter formatter = new ExceptionFormatter(7, theme, status);
        AtomicInteger index = new AtomicInteger(0);
        for (TestCaseFinished testCaseFinished : testCasesFinished) {
            query.findTestCaseStartedBy(testCaseFinished)
                    .flatMap(testCaseStarted ->
                            query.findPickleBy(testCaseStarted)
                                    .map(pickle -> formatScenarioLine(index, testCaseStarted, pickle)))
                    .ifPresent(out::println);
            query.findMostSevereTestStepResultBy(testCaseFinished)
                    .flatMap(TestStepResult::getException)
                    .flatMap(formatter::format)
                    .ifPresent(out::println);
        }
        if (!testCasesFinished.isEmpty()) {
            out.println();
        }
    }

    private String formatScenarioLine(AtomicInteger counter, TestCaseStarted testCaseStarted, Pickle pickle) {
        int index = counter.incrementAndGet();
        // TODO: Use long name?
        String name = pickle.getName();
        String attempt = formatAttempt(testCaseStarted);
        String location = formatLocationComment(pickle);
        return String.format("  %d) %s%s %s", index, name, attempt, location);
    }

    private static String formatAttempt(TestCaseStarted testCaseStarted) {
        Long attempt = testCaseStarted.getAttempt();
        if (attempt == 0) {
            return "";
        }
        return ", after " + attempt + " attempts";
    }

    private String formatLocationComment(Pickle pickle) {
        String formattedUri = uriFormatter.apply(pickle.getUri());
        String comment = "# " + formattedUri + query.findLocationOf(pickle).map(Location::getLine).map(line -> ":" + line).orElse("");
        return theme.style(LOCATION, comment);
    }

    private void printScenarioCounts() {
        out.println(formatSubCounts(
                "Scenarios",
                query.findAllTestCaseFinished(),
                countTestStepResultStatusByTestCaseFinished()));

    }

    private void printStepCounts() {
        out.println(formatSubCounts(
                "Steps",
                // findAllTestCaseFinished excludes non-final test cases
                // This ensures findTestStepsFinishedBy does not include retried steps
                query.findAllTestCaseFinished().stream()
                        .map(query::findTestStepsFinishedBy)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList()), 
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
                joiner.add(theme.style(STATUS_ICON, status, count + " " + status.name().toLowerCase(ROOT)));
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

    private void printSnippets() {
        Set<Snippet> snippets = query.findAllTestCaseFinished().stream()
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
        for (Snippet snippet : snippets) {
            out.println(snippet.getCode());
            out.println();
        }
    }

    private Collector<TestCaseFinished, ?, Map<TestStepResultStatus, Long>> countTestStepResultStatusByTestCaseFinished() {
        return groupingBy(this::getTestStepResultStatusBy, counting());
    }

    private TestStepResultStatus getTestStepResultStatusBy(TestCaseFinished testCaseFinished) {
        return query.findMostSevereTestStepResultBy(testCaseFinished)
                .map(TestStepResult::getStatus)
                // By definition
                .orElse(TestStepResultStatus.PASSED);
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
