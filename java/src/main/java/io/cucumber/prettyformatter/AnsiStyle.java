package io.cucumber.prettyformatter;

/**
 * Select graphic rendition control sequences in the format {@code CSI n m}.
 */
class AnsiStyle {

    static final AnsiStyle INTENSITY_BOLD = new AnsiStyle(Attributes.INTENSITY_BOLD, Attributes.INTENSITY_BOLD_OFF);
    static final AnsiStyle FOREGROUND_RED = new AnsiStyle(Attributes.FOREGROUND_RED, Attributes.RESET);
    static final AnsiStyle FOREGROUND_GREEN = new AnsiStyle(Attributes.FOREGROUND_GREEN, Attributes.RESET);
    static final AnsiStyle FOREGROUND_YELLOW = new AnsiStyle(Attributes.FOREGROUND_YELLOW, Attributes.RESET);
    static final AnsiStyle FOREGROUND_BLUE = new AnsiStyle(Attributes.FOREGROUND_BLUE, Attributes.RESET);
    static final AnsiStyle FOREGROUND_CYAN = new AnsiStyle(Attributes.FOREGROUND_CYAN, Attributes.RESET);
    static final AnsiStyle FOREGROUND_BRIGHT_BLACK = new AnsiStyle(Attributes.FOREGROUND_BRIGHT_BLACK, Attributes.RESET);
    static final AnsiStyle FOREGROUND_DEFAULT = new AnsiStyle(Attributes.FOREGROUND_DEFAULT, Attributes.RESET);
    
    private static final char FIRST_ESCAPE = 27;
    private static final char SECOND_ESCAPE = '[';
    private static final String END_SEQUENCE = "m";
    private final String startControlSequence;
    private final String endControlSequence;

    AnsiStyle(Attributes startAttribute, Attributes endAttribute) {
        startControlSequence = createControlSequence(startAttribute);
        endControlSequence = createControlSequence(endAttribute);
    }

    private String createControlSequence(Attributes... attributes) {
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
        return startControlSequence + " and " + endControlSequence;
    }
    
    String getStartControlSequence() {
        return startControlSequence;
    }

    String getEndControlSequence() {
        return endControlSequence;
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

        int value() {
            return value;
        }
    }
}
