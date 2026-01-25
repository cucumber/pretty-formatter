package io.cucumber.prettyformatter;

import io.cucumber.compatibilitykit.MessageOrderer;
import io.cucumber.messages.NdjsonToMessageReader;
import io.cucumber.messages.ndjson.Deserializer;
import io.cucumber.messages.types.Envelope;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.cucumber.prettyformatter.MessagesToSummaryWriter.SummaryFeature.INCLUDE_ATTACHMENTS;
import static io.cucumber.prettyformatter.Theme.cucumber;
import static io.cucumber.prettyformatter.Theme.plain;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

class MessagesToSummaryWriterAcceptanceTest {

    private static final Random random = new Random(202509171620L);
    private static final MessageOrderer messageOrderer = new MessageOrderer(random);

    static List<TestCase> acceptance() throws IOException {
        Map<String, MessagesToSummaryWriter.Builder> themes = new LinkedHashMap<>();
        themes.put("cucumber", MessagesToSummaryWriter.builder().theme(cucumber()));
        themes.put("plain", MessagesToSummaryWriter.builder().theme(plain()));
        themes.put("exclude-attachments", MessagesToSummaryWriter.builder()
                .theme(plain())
                .feature(INCLUDE_ATTACHMENTS, false));

        List<Path> sources = getSources();

        List<TestCase> testCases = new ArrayList<>();
        sources.forEach(path ->
                themes.forEach((strategyName, strategy) ->
                        testCases.add(new TestCase(path, strategyName, strategy))));

        return testCases;
    }

    private static List<Path> getSources() throws IOException {
        try (Stream<Path> paths = Files.list(Paths.get("..", "testdata", "src"))) {
            return paths
                    .filter(path -> path.getFileName().toString().endsWith(".ndjson"))
                    .collect(Collectors.toList());
        }
    }

    private static ByteArrayOutputStream writeSummaryReport(TestCase testCase, MessagesToSummaryWriter.Builder builder, Consumer<List<Envelope>> orderer) throws IOException {
        return writeSummaryReport(testCase, new ByteArrayOutputStream(), builder, orderer);
    }

    private static <T extends OutputStream> T writeSummaryReport(TestCase testCase, T out, MessagesToSummaryWriter.Builder builder, Consumer<List<Envelope>> orderer) throws IOException {
        try (var in = Files.newInputStream(testCase.source)) {
            try (var reader = new NdjsonToMessageReader(in, new Deserializer())) {
                List<Envelope> messages = reader.lines().collect(Collectors.toList());
                orderer.accept(messages);
                try (var writer = builder.build(out)) {
                    for (Envelope envelope : messages) {
                        writer.write(envelope);
                    }
                }
            }
        }

        return out;
    }

    @ParameterizedTest
    @MethodSource("acceptance")
    void test(TestCase testCase) throws IOException {
        ByteArrayOutputStream bytes = writeSummaryReport(testCase, testCase.builder, messageOrderer.originalOrder());
        assertThat(bytes.toString(UTF_8)).isEqualToIgnoringNewLines(Files.readString(testCase.expected));
    }

    @ParameterizedTest
    @MethodSource("acceptance")
    void testWithSimulatedParallelExecution(TestCase testCase) throws IOException {
        ByteArrayOutputStream bytes = writeSummaryReport(testCase, testCase.builder, messageOrderer.simulateParallelExecution());
        assertThat(bytes.toString(UTF_8)).isEqualToIgnoringNewLines(Files.readString(testCase.expected));
    }

    @ParameterizedTest
    @MethodSource("acceptance")
    @Disabled
    void updateExpectedFiles(TestCase testCase) throws IOException {
        try (OutputStream out = Files.newOutputStream(testCase.expected)) {
            writeSummaryReport(testCase, out, testCase.builder, messageOrderer.originalOrder());
            // Render output in console, easier to inspect results
            Files.copy(testCase.expected, System.out);
        }
    }

    static class TestCase {
        private final Path source;
        private final String themeName;
        private final MessagesToSummaryWriter.Builder builder;
        private final Path expected;

        private final String name;

        TestCase(Path source, String themeName, MessagesToSummaryWriter.Builder builder) {
            this.source = source;
            this.themeName = themeName;
            this.builder = builder;
            String fileName = source.getFileName().toString();
            this.name = fileName.substring(0, fileName.lastIndexOf(".ndjson"));
            this.expected = requireNonNull(source.getParent()).resolve(name + "." + themeName + ".summary.log");
        }

        @Override
        public String toString() {
            return name + " -> " + themeName;
        }

    }

}

