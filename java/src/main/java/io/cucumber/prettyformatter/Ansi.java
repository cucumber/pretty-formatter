package io.cucumber.prettyformatter;

import static java.util.Objects.requireNonNull;

/**
 * Represents an <a href="https://en.wikipedia.org/wiki/ANSI_escape_code">ANSI escape code</a> in the format {@code CSI n m}.
 */
public final class Ansi {
    
    private static final char FIRST_ESCAPE = 27;
    private static final char SECOND_ESCAPE = '[';
    private static final String END_SEQUENCE = "m";
    private final String controlSequence;

    /**
     * Constructs an ANSI escape code with the given attributes.
     * 
     * @param attributes to include.
     * @return an ANSI escape code with the given attributes
     */
    public static Ansi with(Attributes... attributes) {
        return new Ansi(requireNonNull(attributes));
    }
    
    private Ansi(Attributes... attributes) {
        this.controlSequence = createControlSequence(attributes);
    }

    private String createControlSequence(Attributes... attributes) {
        StringBuilder a = new StringBuilder(attributes.length * 5);

        for (Attributes attribute : attributes) {
            a.append(FIRST_ESCAPE).append(SECOND_ESCAPE);
            a.append(attribute.value);
            a.append(END_SEQUENCE);
        }

        return a.toString();
    }

    @Override
    public String toString() {
        return controlSequence;
    }

    /**
     * A select number of attributes from all the available 
     * <a href=https://en.wikipedia.org/wiki/ANSI_escape_code#Select_Graphic_Rendition_parameters>Select Graphic Rendition attributes</a>.
     */
    public enum Attributes {
        RESET(0),
        
        BOLD(1),
        BOLD_OFF(22),
        
        FAINT(2),
        FAINT_OFF(22),
        
        ITALIC(3),
        ITALIC_OFF(23),
        
        UNDERLINE(4),
        UNDERLINE_OFF(24),
        
        INTENSITY_ITALIC(3),
        INTENSITY_ITALIC_OFF(23),

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
        
        BACKGROUND_BLACK(40),
        BACKGROUND_RED(41),
        BACKGROUND_GREEN(42),
        BACKGROUND_YELLOW(43),
        BACKGROUND_BLUE(44),
        BACKGROUND_MAGENTA(45),
        BACKGROUND_CYAN(46),
        BACKGROUND_WHITE(47),
        BACKGROUND_DEFAULT(49),

        FOREGROUND_BRIGHT_BLACK(90),
        FOREGROUND_BRIGHT_RED(91),
        FOREGROUND_BRIGHT_GREEN(92),
        FOREGROUND_BRIGHT_YELLOW(93),
        FOREGROUND_BRIGHT_BLUE(94),
        FOREGROUND_BRIGHT_MAGENTA(95),
        FOREGROUND_BRIGHT_CYAN(96),
        FOREGROUND_BRIGHT_WHITE(97),
        
        BACKGROUND_BRIGHT_BLACK(100),
        BACKGROUND_BRIGHT_RED(101),
        BACKGROUND_BRIGHT_GREEN(102),
        BACKGROUND_BRIGHT_YELLOW(103),
        BACKGROUND_BRIGHT_BLUE(104),
        BACKGROUND_BRIGHT_MAGENTA(105),
        BACKGROUND_BRIGHT_CYAN(106),
        BACKGROUND_BRIGHT_WHITE(107);

        private final int value;

        Attributes(int index) {
            this.value = index;
        }
    }
}
