#ifndef CUCUMBER_PRETTY_FORMATTER_ANSI_HPP
#define CUCUMBER_PRETTY_FORMATTER_ANSI_HPP

#include <cstdint>
#include <initializer_list>
#include <string>

namespace cucumber::pretty_formatter
{
    struct Ansi
    {
        enum class Attribute : std::uint8_t
        {
            reset = 0,

            bold = 1,
            boldOff = 22,

            faint = 2,
            faintOff = 22,

            italic = 3,
            italicOff = 23,

            underline = 4,
            underlineOff = 24,

            intensityItalic = 3,
            intensityItalicOff = 23,

            // https://en.wikipedia.org/wiki/ansi_escape_code#colors
            foregroundBlack = 30,
            foregroundRed = 31,
            foregroundGreen = 32,
            foregroundYellow = 33,
            foregroundBlue = 34,
            foregroundMagenta = 35,
            foregroundCyan = 36,
            foregroundWhite = 37,
            foregroundDefault = 39,

            backgroundBlack = 40,
            backgroundRed = 41,
            backgroundGreen = 42,
            backgroundYellow = 43,
            backgroundBlue = 44,
            backgroundMagenta = 45,
            backgroundCyan = 46,
            backgroundWhite = 47,
            backgroundDefault = 49,

            foregroundBrightBlack = 90,
            foregroundBrightRed = 91,
            foregroundBrightGreen = 92,
            foregroundBrightYellow = 93,
            foregroundBrightBlue = 94,
            foregroundBrightMagenta = 95,
            foregroundBrightCyan = 96,
            foregroundBrightWhite = 97,

            backgroundBrightBlack = 100,
            backgroundBrightRed = 101,
            backgroundBrightGreen = 102,
            backgroundBrightYellow = 103,
            backgroundBrightBlue = 104,
            backgroundBrightMagenta = 105,
            backgroundBrightCyan = 106,
            backgroundBrightWhite = 107,
        };

        Ansi() = default;
        explicit Ansi(std::initializer_list<Attribute> attributes);

        Ansi(const Ansi&) = default;
        Ansi& operator=(const Ansi&) = default;

        Ansi& operator=(Ansi&&) = default;
        Ansi(Ansi&&) = default;

        [[nodiscard]] std::string ToString() const;

    private:
        std::string controlSequence;
    };
}

#endif
