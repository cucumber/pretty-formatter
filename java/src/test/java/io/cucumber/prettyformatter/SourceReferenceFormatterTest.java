package io.cucumber.prettyformatter;

import io.cucumber.messages.types.JavaMethod;
import io.cucumber.messages.types.JavaStackTraceElement;
import io.cucumber.messages.types.Location;
import io.cucumber.messages.types.SourceReference;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Could be replaced by <a href=https://github.com/cucumber/compatibility-kit/issues/131>compatibility-kit#131</a>.
 */
class SourceReferenceFormatterTest {

    private final Function<String, String> uriFormatter = Function.identity();
    private final SourceReferenceFormatter formatter = new SourceReferenceFormatter(uriFormatter);

    @Test
    void none() {
        SourceReference sourceReference = new SourceReference(
                null,
                null,
                null,
                null
        );
        assertThat(formatter.format(sourceReference))
                .isEmpty();
    }

    @Test
    void uri() {
        SourceReference sourceReference = new SourceReference(
                "path/to/example.feature",
                null,
                null,
                null
        );
        assertThat(formatter.format(sourceReference))
                .contains("path/to/example.feature");
    }

    @Test
    void uri_with_formatter() {
        Function<String, String> uriFormatter = removePrefix("path/to/");
        SourceReferenceFormatter formatter = new SourceReferenceFormatter(uriFormatter);

        SourceReference sourceReference = new SourceReference(
                "path/to/example.feature",
                null,
                null,
                null
        );
        assertThat(formatter.format(sourceReference))
                .contains("example.feature");
    }

    private static Function<String, String> removePrefix(String prefix) {
        return s -> s.startsWith(prefix) ? s.substring(prefix.length()) : s;
    }

    @Test
    void uri_with_location() {
        SourceReference sourceReference = new SourceReference(
                "path/to/example.feature",
                null,
                null,
                new Location(31415, 42)
        );
        assertThat(formatter.format(sourceReference))
                .contains("path/to/example.feature:31415");
    }

    @Test
    void method() {
        SourceReference sourceReference = new SourceReference(
                null,
                new JavaMethod(
                        "org.example.Example",
                        "example",
                        asList("java.lang.String", "java.lang.String")
                ),
                null,
                null
        );
        assertThat(formatter.format(sourceReference))
                .contains("org.example.Example.example(java.lang.String,java.lang.String)");
    }

    @Test
    void method_no_arguments() {
        SourceReference sourceReference = new SourceReference(
                null,
                new JavaMethod(
                        "org.example.Example",
                        "example",
                        emptyList()
                ),
                null,
                null
        );
        assertThat(formatter.format(sourceReference))
                .contains("org.example.Example.example()");
    }

    @Test
    void stacktrace() {
        SourceReference sourceReference = new SourceReference(
                null,
                null,
                new JavaStackTraceElement(
                        "org.example.Example",
                        "path/to/org/example/Example.java",
                        "example"
                ),
                new Location(31415, 42)
        );
        assertThat(formatter.format(sourceReference))
                .contains("org.example.Example.example(path/to/org/example/Example.java:31415)");
    }

}
