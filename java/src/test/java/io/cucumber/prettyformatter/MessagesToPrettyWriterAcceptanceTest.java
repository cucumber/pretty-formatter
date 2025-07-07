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
import static io.cucumber.prettyformatter.MessagesToPrettyWriter.PrettyFeature.INCLUDE_FEATURE_LINE;
import static io.cucumber.prettyformatter.MessagesToPrettyWriter.PrettyFeature.INCLUDE_RULE_LINE;
import static io.cucumber.prettyformatter.MessagesToPrettyWriter.builder;
import static io.cucumber.prettyformatter.TestTheme.demo;
import static io.cucumber.prettyformatter.Theme.cucumber;
import static io.cucumber.prettyformatter.Theme.none;
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
    void testCucumberTheme(TestCase testCase) throws IOException {
        Builder builder = builder().theme(cucumber());
        ByteArrayOutputStream bytes = writePrettyReport(testCase, new ByteArrayOutputStream(), builder);
        assertThat(bytes.toString()).isEqualToIgnoringNewLines(new String(readAllBytes(testCase.expectedCumberTheme)));
    }

    @ParameterizedTest
    @MethodSource("acceptance")
    void testDemoTheme(TestCase testCase) throws IOException {
        Builder theme = builder().theme(demo());
        ByteArrayOutputStream bytes = writePrettyReport(testCase, new ByteArrayOutputStream(), theme);
        assertThat(bytes.toString()).isEqualToIgnoringNewLines(new String(readAllBytes(testCase.expectedDemoTheme)));
    }

    @ParameterizedTest
    @MethodSource("acceptance")
    void testNoTheme(TestCase testCase) throws IOException {
        Builder builder = builder().theme(none());
        ByteArrayOutputStream bytes = writePrettyReport(testCase, new ByteArrayOutputStream(), builder);
        assertThat(bytes.toString()).isEqualToIgnoringNewLines(new String(readAllBytes(testCase.expectedNoTheme)));
    }

    @ParameterizedTest
    @MethodSource("acceptance")
    void testExcludeFeaturesAndRules(TestCase testCase) throws IOException {
        Builder builder = builder().theme(none())
                .feature(INCLUDE_FEATURE_LINE, false)
                .feature(INCLUDE_RULE_LINE, false);
        ByteArrayOutputStream bytes = writePrettyReport(testCase, new ByteArrayOutputStream(), builder);
        assertThat(bytes.toString()).isEqualToIgnoringNewLines(new String(readAllBytes(testCase.expectedExcludeFeatureAndRuleLines)));
    }

    @ParameterizedTest
    @MethodSource("acceptance")
    @Disabled
    void updateExpectedPrettyFiles(TestCase testCase) throws IOException {
        try (OutputStream out = Files.newOutputStream(testCase.expectedCumberTheme)) {
            Builder builder = builder().theme(cucumber());
            writePrettyReport(testCase, out, builder);
            // Render output in console, easier to inspect results
            Files.copy(testCase.expectedCumberTheme, System.out);
        }
        try (OutputStream out = Files.newOutputStream(testCase.expectedDemoTheme)) {
            Builder builder = builder().theme(demo());
            writePrettyReport(testCase, out, builder);
            // Render output in console, easier to inspect results
            // Files.copy(testCase.expectedDemoTheme, System.out);
        }
        try (OutputStream out = Files.newOutputStream(testCase.expectedNoTheme)) {
            Builder builder = builder().theme(none());
            writePrettyReport(testCase, out, builder);
            // Render output in console, easier to inspect results
            // Files.copy(testCase.expectedNoTheme, System.out);
        }
        try (OutputStream out = Files.newOutputStream(testCase.expectedExcludeFeatureAndRuleLines)) {
            Builder builder = builder().theme(none())
                    .feature(INCLUDE_FEATURE_LINE, false)
                    .feature(INCLUDE_RULE_LINE, false);
            writePrettyReport(testCase, out, builder);
            // Render output in console, easier to inspect results
            // Files.copy(testCase.expectedExcludeFeaturesAndRules, System.out);
        }
    }

    static class TestCase {
        private final Path source;
        private final Path expectedCumberTheme;
        private final Path expectedNoTheme;
        private final Path expectedDemoTheme;
        private final Path expectedExcludeFeatureAndRuleLines;

        private final String name;

        TestCase(Path source) {
            this.source = source;
            String fileName = source.getFileName().toString();
            this.name = fileName.substring(0, fileName.lastIndexOf(".ndjson"));
            this.expectedCumberTheme = source.getParent().resolve(name + ".cucumber.log");
            this.expectedNoTheme = source.getParent().resolve(name + ".none.log");
            this.expectedDemoTheme = source.getParent().resolve(name + ".demo.log");
            this.expectedExcludeFeatureAndRuleLines = source.getParent().resolve(name + ".exclude-features-and-rules.log");
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

