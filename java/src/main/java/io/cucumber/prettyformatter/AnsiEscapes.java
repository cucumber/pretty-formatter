package io.cucumber.prettyformatter;

final class AnsiEscapes {
    static final AnsiEscapes RESET = code(0);
    static final AnsiEscapes DEFAULT = code(9);
    static final AnsiEscapes INTENSITY_BOLD = code(1);
    static final AnsiEscapes UNDERLINE = code(4);
    static final AnsiEscapes RESET_INTENSITY_BOLD = code(22);
    static final AnsiEscapes BLACK = code(30);
    static final AnsiEscapes RED = code(31);
    static final AnsiEscapes GREEN = code(32);
    static final AnsiEscapes YELLOW = code(33);
    static final AnsiEscapes BLUE = code(34);
    static final AnsiEscapes MAGENTA = code(35);
    static final AnsiEscapes CYAN = code(36);
    static final AnsiEscapes WHITE = code(37);
    static final AnsiEscapes GREY = code(90);

    private static final char ESC = 27;
    private static final char BRACKET = '[';
    private final int code;

    private AnsiEscapes(int code) {
        this.code = code;
    }

    private static AnsiEscapes code(int code) {
        return new AnsiEscapes(code);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        appendTo(sb);
        return sb.toString();
    }

    void appendTo(StringBuilder a) {
        a.append(ESC).append(BRACKET).append(code).append(  "m");
    }

}
