package io.cucumber.prettyformatter;

import io.cucumber.messages.NdjsonToMessageIterable;
import io.cucumber.messages.types.Envelope;
import io.cucumber.prettyformatter.MessagesToPrettyWriter.Builder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.cucumber.prettyformatter.Jackson.OBJECT_MAPPER;
import static io.cucumber.prettyformatter.MessagesToPrettyWriter.builder;
import static io.cucumber.prettyformatter.TestTheme.cucumberJs;
import static io.cucumber.prettyformatter.Theme.noColor;
import static java.nio.file.Files.readAllBytes;
import static org.assertj.core.api.Assertions.assertThat;

class MessagesToPrettyWriterAcceptanceTest {
    private static final NdjsonToMessageIterable.Deserializer deserializer = (json) -> OBJECT_MAPPER.readValue(json, Envelope.class);

    static List<TestCase> acceptance() throws IOException {
        try (Stream<Path> paths = Files.list(Paths.get("../testdata"))) {
            return paths
                    .filter(path -> path.getFileName().toString().endsWith(".ndjson"))
                    .map(TestCase::new)
                    .sorted(Comparator.comparing(testCase -> testCase.source))
                    .collect(Collectors.toList());
        }
    }

    private static <T extends OutputStream> T writePrettyReport(TestCase testCase, T out, Builder builder) throws IOException {
        try (InputStream in = Files.newInputStream(testCase.source)) {
            try (NdjsonToMessageIterable envelopes = new NdjsonToMessageIterable(in, deserializer)) {
                try (MessagesToPrettyWriter writer = builder.build(out)) {
                    for (Envelope envelope : envelopes) {
                        writer.write(envelope);
                    }
                }
            }
        }
        return out;
    }

    @ParameterizedTest
    @MethodSource("acceptance")
    void testCucumberJvm(TestCase testCase) throws IOException {
        ByteArrayOutputStream bytes = writePrettyReport(testCase, new ByteArrayOutputStream(), builder());
        assertThat(bytes.toString()).isEqualToIgnoringNewLines(new String(readAllBytes(testCase.expectedCucumberJvm)));
    }

    @ParameterizedTest
    @MethodSource("acceptance")
    void testCucumberJs(TestCase testCase) throws IOException {
        ByteArrayOutputStream bytes = writePrettyReport(testCase, new ByteArrayOutputStream(), builder().theme(cucumberJs()));
        assertThat(bytes.toString()).isEqualToIgnoringNewLines(new String(readAllBytes(testCase.expectedCucumberJs)));
    }

    @ParameterizedTest
    @MethodSource("acceptance")
    void testNoColor(TestCase testCase) throws IOException {
        ByteArrayOutputStream bytes = writePrettyReport(testCase, new ByteArrayOutputStream(), builder().theme(noColor()));
        assertThat(bytes.toString()).isEqualToIgnoringNewLines(new String(readAllBytes(testCase.expectedNoColor)));
    }

    @ParameterizedTest
    @MethodSource("acceptance")
    @Disabled
    void updateExpectedPrettyFiles(TestCase testCase) throws IOException {
        try (OutputStream out = Files.newOutputStream(testCase.expectedCucumberJvm)) {
            writePrettyReport(testCase, out, builder());
            // Render output in console, easier to inspect results
            Files.copy(testCase.expectedCucumberJvm, System.out);
        }
        try (OutputStream out = Files.newOutputStream(testCase.expectedCucumberJs)) {
            writePrettyReport(testCase, out, builder().theme(cucumberJs()));
            // Render output in console, easier to inspect results
            // Files.copy(testCase.expectedCucumberJs, System.out);
        }
        try (OutputStream out = Files.newOutputStream(testCase.expectedNoColor)) {
            writePrettyReport(testCase, out, builder().theme(noColor()));
            // Render output in console, easier to inspect results
            // Files.copy(testCase.expectedNoColor, System.out);
        }
        try (OutputStream out = Files.newOutputStream(testCase.expectedExcludeFeaturesAndRules)) {
            writePrettyReport(testCase, out, builder().theme(noColor()).includeFeatureAndRules(false));
            // Render output in console, easier to inspect results
            // Files.copy(testCase.expectedExcludeFeaturesAndRules, System.out);
        }
    }

    static class TestCase {
        private final Path source;
        private final Path expectedCucumberJvm;
        private final Path expectedNoColor;
        private final Path expectedCucumberJs;
        private final Path expectedExcludeFeaturesAndRules;

        private final String name;

        TestCase(Path source) {
            this.source = source;
            String fileName = source.getFileName().toString();
            this.name = fileName.substring(0, fileName.lastIndexOf(".ndjson"));
            this.expectedCucumberJvm = source.getParent().resolve(name + "cucumber-jvm.log");
            this.expectedNoColor = source.getParent().resolve(name + ".no-color.log");
            this.expectedCucumberJs = source.getParent().resolve(name + ".cucumber-js.log");
            this.expectedExcludeFeaturesAndRules = source.getParent().resolve(name + ".exclude-features-and-rules.log");
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestCase testCase = (TestCase) o;
            return source.equals(testCase.source);
        }

        @Override
        public int hashCode() {
            return Objects.hash(source);
        }
    }

}

