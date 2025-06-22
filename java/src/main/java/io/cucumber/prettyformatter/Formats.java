package io.cucumber.prettyformatter;

import io.cucumber.messages.types.TestStepResultStatus;

import java.util.EnumMap;

import static io.cucumber.messages.types.TestStepResultStatus.AMBIGUOUS;
import static io.cucumber.messages.types.TestStepResultStatus.FAILED;
import static io.cucumber.messages.types.TestStepResultStatus.PASSED;
import static io.cucumber.messages.types.TestStepResultStatus.PENDING;
import static io.cucumber.messages.types.TestStepResultStatus.SKIPPED;
import static io.cucumber.messages.types.TestStepResultStatus.UNDEFINED;
import static io.cucumber.prettyformatter.Format.color;

interface Formats {

    Format status(TestStepResultStatus status);
    Format comment();
    Format tag();
    Format output();

    static Formats monochrome() {
        return new Monochrome();
    }

    static Formats ansi() {
        return new Ansi();
    }

    final class Monochrome implements Formats {
        
        private final Format noop = new Format() {
            
        };
        
        private Monochrome() {

        }

        @Override
        public Format status(TestStepResultStatus status) {
            return noop;
        }

        @Override
        public Format comment() {
            return noop;
        }

        @Override
        public Format tag() {
            return noop;
        }

        @Override
        public Format output() {
            return noop;
        }
    }

    final class Ansi implements Formats {

        private final Format unknown = new Format.Monochrome();
        private final Format comment = color(AnsiEscapes.GREY);
        private final Format tag = color(AnsiEscapes.CYAN);
        private final Format output = color(AnsiEscapes.BLUE);

        private final EnumMap<TestStepResultStatus, Format> formats;

        {
            EnumMap<TestStepResultStatus, Format> tmp = new EnumMap<>(TestStepResultStatus.class);
            // Never used, but avoids NPE in formatters.
            tmp.put(UNDEFINED, color(AnsiEscapes.YELLOW));
            tmp.put(PENDING, color(AnsiEscapes.YELLOW));
            tmp.put(FAILED, color(AnsiEscapes.RED));
            tmp.put(AMBIGUOUS, color(AnsiEscapes.RED));
            tmp.put(PASSED, color(AnsiEscapes.GREEN));
            tmp.put(SKIPPED, color(AnsiEscapes.CYAN));
            formats = tmp;
        }

        private Ansi() {

        }

        public Format status(TestStepResultStatus status) {
            return formats.getOrDefault(status, unknown);
        }

        @Override
        public Format comment() {
            return comment;
        }

        @Override
        public Format tag() {
            return tag;
        }

        @Override
        public Format output() {
            return output;
        }

    }

}
