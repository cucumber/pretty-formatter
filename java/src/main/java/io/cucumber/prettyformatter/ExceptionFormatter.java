package io.cucumber.prettyformatter;

import io.cucumber.messages.types.Exception;
import io.cucumber.messages.types.TestStepResultStatus;
import org.jspecify.annotations.Nullable;

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

    Optional<String> format(Exception exception, @Nullable String standaloneMessage) {
        // For FAILED, prefer stack trace, fall back to message
        if (status == TestStepResultStatus.FAILED) {
            if (exception.getStackTrace().isPresent()) {
                return Optional.of(format(exception.getStackTrace().get()));
            }
            if (exception.getMessage().isPresent()) {
                return Optional.of(format(exception.getMessage().get()));
            }
            return Optional.ofNullable(standaloneMessage).map(this::format);
        }

        // For PENDING/SKIPPED, only show the message (not stack trace)
        if (status == TestStepResultStatus.PENDING || status == TestStepResultStatus.SKIPPED) {
            if (exception.getMessage().isPresent()) {
                return Optional.of(format(exception.getMessage().get()));
            }
            return Optional.ofNullable(standaloneMessage).map(this::format);
        }

        // For all other statuses, return nothing
        return Optional.empty();
    }

    Optional<String> format(Exception exception) {
        return format(exception, null);
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
