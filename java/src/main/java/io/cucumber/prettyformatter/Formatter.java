package io.cucumber.prettyformatter;

import io.cucumber.messages.types.TestStepResultStatus;

import java.util.EnumMap;

import static io.cucumber.messages.types.TestStepResultStatus.AMBIGUOUS;
import static io.cucumber.messages.types.TestStepResultStatus.FAILED;
import static io.cucumber.messages.types.TestStepResultStatus.PASSED;
import static io.cucumber.messages.types.TestStepResultStatus.PENDING;
import static io.cucumber.messages.types.TestStepResultStatus.SKIPPED;
import static io.cucumber.messages.types.TestStepResultStatus.UNDEFINED;

interface Formatter {
    
    String comment(String text);
    
    String status(TestStepResultStatus status, String text);
    
    String argument(String text);

    String output(String text);
    
    class Ansi implements Formatter {

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
            return AnsiEscapes.FOREGROUND_BRIGHT_BLACK + text + AnsiEscapes.RESET;
        }

        @Override
        public String status(TestStepResultStatus status, String text) {
            return formats.getOrDefault(status, AnsiEscapes.FOREGROUND_DEFAULT) + text + AnsiEscapes.RESET;
        }

        @Override
        public String argument(String text) {
            return AnsiEscapes.INTENSITY_BOLD + text + AnsiEscapes.INTENSITY_BOLD_OFF;
        }

        @Override
        public String output(String text) {
            return AnsiEscapes.FOREGROUND_BLUE + text + AnsiEscapes.RESET;
        }
    }
    
    class NoAnsi implements Formatter {

        @Override
        public String comment(String text) {
            return text;
        }

        @Override
        public String status(TestStepResultStatus status, String text) {
            return text;
        }

        @Override
        public String argument(String text) {
            return text;
        }

        @Override
        public String output(String text) {
            return text;
        }
    }
}
