package io.cucumber.prettyformatter;

import io.cucumber.messages.types.Exception;
import io.cucumber.messages.types.TestStepResultStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Optional;

import static io.cucumber.prettyformatter.Theme.Element.STEP;

final class ExceptionFormatter {

    private final int indent;
    private final Theme theme;
    private final TestStepResultStatus status;

    ExceptionFormatter(int indent, Theme theme, TestStepResultStatus status) {
        this.indent = indent;
        this.theme = theme;
        this.status = status;
    }

    Optional<String> format(Exception exception) {
        if (exception.getStackTrace().isPresent()) {
            String stacktrace = exception.getStackTrace().get();
            return Optional.of(format(stacktrace));
        }
        // Fallback
        if (exception.getMessage().isPresent()) {
            String message = exception.getMessage().get();
            return Optional.of(format(message));
        }
        return Optional.empty();
    }

    String format(String message) {
        LineBuilder builder = new LineBuilder(theme);
        // Read the lines in the message and add extra indentation
        try (BufferedReader lines = new BufferedReader(new StringReader(message))) {
            String line;
            while ((line = lines.readLine()) != null) {
                builder.indent(indent)
                        .append(STEP, status, line)
                        .newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return builder
                .build();
    }
}
