package io.cucumber.prettyformatter;

import io.cucumber.messages.types.TestRunFinished;
import io.cucumber.messages.types.TestRunHookFinished;
import io.cucumber.messages.types.TestStepFinished;
import io.cucumber.messages.types.TestStepResultStatus;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import static io.cucumber.prettyformatter.Theme.Element.PROGRESS_ICON;
import static java.lang.System.lineSeparator;
import static java.util.Objects.requireNonNull;

final class ProgressWriter implements AutoCloseable {

    private final PrintWriter writer;
    private final Theme theme;
    private final int maxWidth;
    private int width = 0;

    ProgressWriter(OutputStream out, Theme theme, int maxWidth) {
        this.writer = createPrintWriter(out);
        this.theme = requireNonNull(theme);
        this.maxWidth = maxWidth;
    }

    private static PrintWriter createPrintWriter(OutputStream out) {
        return new PrintWriter(
                new OutputStreamWriter(
                        requireNonNull(out),
                        StandardCharsets.UTF_8
                ),
                true
        );
    }

    @Override
    public void close() {
        writer.close();
    }

    void write(TestRunHookFinished event) {
        printStatus(event.getResult().getStatus());

    }

    void write(TestStepFinished event) {
        printStatus(event.getTestStepResult().getStatus());
    }

    private void printStatus(TestStepResultStatus status) {
        // Prevent tearing in output when multiple threads write to System.out
        StringBuilder buffer = new StringBuilder();
        String icon = theme.progressIcon(status);
        buffer.append(theme.style(PROGRESS_ICON, status, icon));
        // Start a new line if at the end of this one
        if (++width % maxWidth == 0) {
            width = 0;
            buffer.append(lineSeparator());
        }
        writer.append(buffer);
        // Flush to provide immediate feedback.
        writer.flush();
    }

    void write(TestRunFinished testRunHookFinished) {
        writer.println();
    }
}
