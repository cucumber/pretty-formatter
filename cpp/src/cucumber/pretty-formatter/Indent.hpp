#ifndef CUCUMBER_PRETTY_FORMATTER_INDENT_HPP
#define CUCUMBER_PRETTY_FORMATTER_INDENT_HPP

#include <cstddef>
#include <fmt/core.h>
#include <fmt/format.h>
#include <string>

namespace cucumber::pretty_formatter
{
    inline std::string Indent(std::size_t spaces, const std::string& str);
}

#endif
