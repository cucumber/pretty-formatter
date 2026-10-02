#ifndef CUCUMBER_PRETTY_FORMATTER_CASE_UTIL_HPP
#define CUCUMBER_PRETTY_FORMATTER_CASE_UTIL_HPP

#include <string>
#include <string_view>

namespace cucumber::pretty_formatter
{
    char ToLower(char chr);
    std::string ToLower(std::string text);
    std::string ToLower(std::string_view text);

    char ToUpper(char chr);
    std::string ToUpper(std::string text);
    std::string ToUpper(std::string_view text);

    std::string SentenceCase(std::string text);
    std::string SentenceCase(std::string_view text);
}

#endif
