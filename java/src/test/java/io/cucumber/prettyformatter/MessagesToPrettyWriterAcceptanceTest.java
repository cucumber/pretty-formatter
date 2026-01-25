package io.cucumber.prettyformatter;

import io.cucumber.messages.NdjsonToMessageReader;
import io.cucumber.messages.ndjson.Deserializer;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.cucumber.prettyformatter.MessagesToPrettyWriter.PrettyFeature.INCLUDE_ATTACHMENTS;
import static io.cucumber.prettyformatter.MessagesToPrettyWriter.PrettyFeature.INCLUDE_FEATURE_LINE;
import static io.cucumber.prettyformatter.MessagesToPrettyWriter.PrettyFeature.INCLUDE_RULE_LINE;
import static io.cucumber.prettyformatter.TestTheme.demo;
import static io.cucumber.prettyformatter.Theme.cucumber;
import static io.cucumber.prettyformatter.Theme.none;
import static io.cucumber.prettyformatter.Theme.plain;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

class MessagesToPrettyWriterAcceptanceTest {

    static List<TestCase> acceptance() throws IOException {
        Map<String, MessagesToPrettyWriter.Builder> themes = new LinkedHashMap<>();
        themes.put("cucumber", MessagesToPrettyWriter.builder().theme(cucumber()));
        themes.put("demo", MessagesToPrettyWriter.builder().theme(demo()));
        themes.put("plain", MessagesToPrettyWriter.builder().theme(plain()));
        themes.put("none", MessagesToPrettyWriter.builder().theme(none()));
        themes.put("exclude-features-and-rules", MessagesToPrettyWriter.builder()
                .theme(none())
                .feature(INCLUDE_RULE_LINE, false)
                .feature(INCLUDE_FEATURE_LINE, false));
        themes.put("exclude-attachments", MessagesToPrettyWriter.builder()
                .theme(none())
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

    private static <T extends OutputStream> T writePrettyReport(TestCase testCase, T out, MessagesToPrettyWriter.Builder builder) throws IOException {
        try (var in = Files.newInputStream(testCase.source)) {
            try (var reader = new NdjsonToMessageReader(in, new Deserializer())) {
                try (var writer = builder.build(out)) {
                    for (var envelope : reader.lines().toList()) {
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
        ByteArrayOutputStream bytes = writePrettyReport(testCase, new ByteArrayOutputStream(), testCase.builder);
        assertThat(bytes.toString(UTF_8)).isEqualToIgnoringNewLines(Files.readString(testCase.expected));
    }

    @ParameterizedTest
    @MethodSource("acceptance")
    @Disabled
    void updateExpectedFiles(TestCase testCase) throws IOException {
        try (OutputStream out = Files.newOutputStream(testCase.expected)) {
            writePrettyReport(testCase, out, testCase.builder);
            // Render output in console, easier to inspect results
            Files.copy(testCase.expected, System.out);
        }
    }

    static class TestCase {
        private final Path source;
        private final String themeName;
        private final MessagesToPrettyWriter.Builder builder;
        private final Path expected;

        private final String name;

        TestCase(Path source, String themeName, MessagesToPrettyWriter.Builder builder) {
            this.source = source;
            this.themeName = themeName;
            this.builder = builder;
            String fileName = source.getFileName().toString();
            this.name = fileName.substring(0, fileName.lastIndexOf(".ndjson"));
            this.expected = requireNonNull(source.getParent()).resolve(name + "." + themeName + ".pretty.log");
        }

        @Override
        public String toString() {
            return name + " -> " + themeName;
        }

    }

}

