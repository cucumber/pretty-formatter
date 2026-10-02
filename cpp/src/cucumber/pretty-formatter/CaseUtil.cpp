#include "cucumber/pretty-formatter/CaseUtil.hpp"
#include <algorithm>
#include <cctype>
#include <string>
#include <string_view>

namespace cucumber::pretty_formatter
{
    char ToLower(char chr)
    {
        return static_cast<char>(std::tolower(chr));
    }

    std::string ToLower(std::string text)
    {
        std::transform(text.begin(), text.end(), text.begin(), static_cast<char (*)(char)>(ToLower));
        return text;
    }

    std::string ToLower(std::string_view text)
    {
        return ToLower(std::string{ text });
    }

    char ToUpper(char chr)
    {
        return static_cast<char>(std::toupper(chr));
    }

    std::string ToUpper(std::string text)
    {
        std::transform(text.begin(), text.end(), text.begin(), static_cast<char (*)(char)>(ToUpper));
        return text;
    }

    std::string ToUpper(std::string_view text)
    {
        return ToUpper(std::string{ text });
    }

    std::string SentenceCase(std::string text)
    {
        if (text.empty())
        {
            return text;
        }

        std::transform(text.begin(), text.end(), text.begin(), static_cast<char (*)(char)>(ToLower));

        text.front() = ToUpper(text.front());

        return text;
    }

    std::string SentenceCase(std::string_view text)
    {
        return SentenceCase(std::string{ text });
    }
}
