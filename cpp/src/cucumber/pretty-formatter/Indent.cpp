#include "cucumber/pretty-formatter/Indent.hpp"
#include <cstddef>
#include <fmt/core.h>
#include <fmt/format.h>
#include <fmt/ranges.h>
#include <sstream>
#include <string>
#include <vector>

namespace cucumber::pretty_formatter
{
    inline std::string Indent(std::size_t spaces, const std::string& str)
    {
        std::vector<std::string> lines;
        std::istringstream stream{ str };
        for (std::string line; std::getline(stream, line);)
        {
            lines.emplace_back(std::string(spaces, ' ') + line);
        }

        return fmt::format("{}", fmt::join(lines, "\n"));
    }
}
