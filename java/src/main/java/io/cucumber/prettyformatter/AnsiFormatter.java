package io.cucumber.prettyformatter;

import io.cucumber.messages.types.TestStepResultStatus;

import java.util.EnumMap;

import static io.cucumber.messages.types.TestStepResultStatus.AMBIGUOUS;
import static io.cucumber.messages.types.TestStepResultStatus.FAILED;
import static io.cucumber.messages.types.TestStepResultStatus.PASSED;
import static io.cucumber.messages.types.TestStepResultStatus.PENDING;
import static io.cucumber.messages.types.TestStepResultStatus.SKIPPED;
import static io.cucumber.messages.types.TestStepResultStatus.UNDEFINED;
import static io.cucumber.prettyformatter.AnsiEscapes.FOREGROUND_BLUE;
import static io.cucumber.prettyformatter.AnsiEscapes.FOREGROUND_BRIGHT_BLACK;
import static io.cucumber.prettyformatter.AnsiEscapes.FOREGROUND_DEFAULT;
import static io.cucumber.prettyformatter.AnsiEscapes.INTENSITY_BOLD;
import static io.cucumber.prettyformatter.AnsiEscapes.INTENSITY_BOLD_OFF;
import static io.cucumber.prettyformatter.AnsiEscapes.RESET;

class AnsiFormatter implements Formatter {

    private final EnumMap<TestStepResultStatus, AnsiEscapes> formats;

    {
        EnumMap<TestStepResultStatus, AnsiEscapes> tmp = new EnumMap<>(TestStepResultStatus.class);
        // Never used, but avoids NPE in formatters.
        tmp.put(UNDEFINED, AnsiEscapes.FOREGROUND_YELLOW);
        tmp.put(PENDING, AnsiEscapes.FOREGROUND_YELLOW);
        tmp.put(FAILED, AnsiEscapes.FOREGROUND_RED);
        tmp.put(AMBIGUOUS, AnsiEscapes.FOREGROUND_RED);
        tmp.put(PASSED, AnsiEscapes.FOREGROUND_GREEN);
        tmp.put(SKIPPED, AnsiEscapes.FOREGROUND_CYAN);
        formats = tmp;
    }

    @Override
    public String comment(String text) {
        return FOREGROUND_BRIGHT_BLACK + text + RESET;
    }

    @Override
    public String scenario(String text) {
        return text;
    }

    @Override
    public String step(TestStepResultStatus status, String text) {
        return formats.getOrDefault(status, FOREGROUND_DEFAULT) + text + RESET;
    }

    @Override
    public String error(TestStepResultStatus status, String text) {
        return step(status, text);
    }

    @Override
    public String argument(String text) {
        return INTENSITY_BOLD + text + INTENSITY_BOLD_OFF;
    }

    @Override
    public String output(String text) {
        return FOREGROUND_BLUE + text + RESET;
    }
}
