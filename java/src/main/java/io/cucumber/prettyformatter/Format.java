package io.cucumber.prettyformatter;

import static io.cucumber.prettyformatter.AnsiEscapes.INTENSITY_BOLD;
import static io.cucumber.prettyformatter.AnsiEscapes.RESET_INTENSITY_BOLD;

interface Format {

    default String color(String text) {
        return text;
    }
    
    default String bold(String text) {
        return text;
    }
    
    static Format color(AnsiEscapes escapes) {
        return new Color(escapes);
    }

    static Format monochrome() {
        return new Monochrome();
    }

    final class Color implements Format {

        private final AnsiEscapes escapes;

        private Color(AnsiEscapes escapes) {
            this.escapes = escapes;
        }

        public String color(String text) {
            StringBuilder sb = new StringBuilder();
            escapes.appendTo(sb);
            sb.append(text);
            AnsiEscapes.RESET.appendTo(sb);
            return sb.toString();
        }

        @Override
        public String bold(String text) {
            StringBuilder sb = new StringBuilder();
            INTENSITY_BOLD.appendTo(sb);
            sb.append(text);
            RESET_INTENSITY_BOLD.appendTo(sb);
            return sb.toString();
        }
    }

    final class Monochrome implements Format {

        Monochrome() {

        }
    }

}
