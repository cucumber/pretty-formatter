package io.cucumber.prettyformatter;

/**
 * Select graphic rendition control sequences in the format {@code CSI n m}.
 */
class AnsiEscapes {

    static final AnsiEscapes RESET = new AnsiEscapes(Attributes.RESET);
    static final AnsiEscapes INTENSITY_BOLD = new AnsiEscapes(Attributes.INTENSITY_BOLD);
    static final AnsiEscapes INTENSITY_BOLD_OFF = new AnsiEscapes(Attributes.INTENSITY_BOLD_OFF);
    static final AnsiEscapes FOREGROUND_RED = new AnsiEscapes(Attributes.FOREGROUND_RED);
    static final AnsiEscapes FOREGROUND_GREEN = new AnsiEscapes(Attributes.FOREGROUND_GREEN);
    static final AnsiEscapes FOREGROUND_YELLOW = new AnsiEscapes(Attributes.FOREGROUND_YELLOW);
    static final AnsiEscapes FOREGROUND_BLUE = new AnsiEscapes(Attributes.FOREGROUND_BLUE);
    static final AnsiEscapes FOREGROUND_CYAN = new AnsiEscapes(Attributes.FOREGROUND_CYAN);
    static final AnsiEscapes FOREGROUND_BRIGHT_BLACK = new AnsiEscapes(Attributes.FOREGROUND_BRIGHT_BLACK);
    static final AnsiEscapes FOREGROUND_DEFAULT = new AnsiEscapes(Attributes.FOREGROUND_DEFAULT);
    
    private static final char FIRST_ESCAPE = 27;
    private static final char SECOND_ESCAPE = '[';
    private static final String END_SEQUENCE = "m";
    private final String controlSequence;

    AnsiEscapes(Attributes... attributes) {
        controlSequence = createControlSequence(attributes);
    }

    private String createControlSequence(Attributes[] attributes) {
        int length = attributes.length;
        int capacity = (length * 2 - 1) + 3;
        StringBuilder a = new StringBuilder(capacity);
        a.append(FIRST_ESCAPE).append(SECOND_ESCAPE);
        for (int i = 0; i < length; i++) {
            Attributes attribute = attributes[i];
            if (i > 0) {
                a.append(";");
            }
            a.append(attribute.value);
        }
        a.append(END_SEQUENCE);
        return a.toString();
    }

    @Override
    public String toString() {
        return controlSequence;
    }

    /**
     * A select number of attributes from all the available <a href=https://en.wikipedia.org/wiki/ANSI_escape_code#Select_Graphic_Rendition_parameters>Select Graphic Rendition attributes</a>.
     */
    enum Attributes {
        RESET(0),
        INTENSITY_BOLD(1),
        INTENSITY_BOLD_OFF(22),

        // https://en.wikipedia.org/wiki/ANSI_escape_code#Colors
        FOREGROUND_BLACK(30),
        FOREGROUND_RED(31),
        FOREGROUND_GREEN(32),
        FOREGROUND_YELLOW(33),
        FOREGROUND_BLUE(34),
        FOREGROUND_MAGENTA(35),
        FOREGROUND_CYAN(36),
        FOREGROUND_WHITE(37),
        FOREGROUND_DEFAULT(39),

        FOREGROUND_BRIGHT_BLACK(90);

        private final int value;

        Attributes(int index) {
            this.value = index;
        }

        public int value() {
            return value;
        }
    }
}
