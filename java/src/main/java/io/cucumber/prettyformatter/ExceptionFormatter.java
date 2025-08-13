package io.cucumber.prettyformatter;

import io.cucumber.messages.types.Exception;
import io.cucumber.messages.types.TestStepResultStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import static io.cucumber.prettyformatter.Theme.Element.STEP;

final class ExceptionFormatter {

    private final int indent;
    private final Theme theme;

    ExceptionFormatter(int indent, Theme theme) {
        this.indent = indent;
        this.theme = theme;
    }

    String format(Exception exception, TestStepResultStatus status) {
        LineBuilder lineBuilder = new LineBuilder(theme);
        if (exception.getStackTrace().isPresent()) {
            String stacktrace = exception.getStackTrace().get();
            return formatMessage(lineBuilder, stacktrace, status);
        }
        if (exception.getMessage().isPresent()) {
            String message = exception.getMessage().get();
            return formatMessage(lineBuilder, message, status);
        }
        return "";
    }

    private String formatMessage(LineBuilder builder, String message, TestStepResultStatus status) {
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
