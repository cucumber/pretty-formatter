#ifndef CUCUMBER_PRETTY_FORMATTER_FORMAT_SNIPPETS_HPP
#define CUCUMBER_PRETTY_FORMATTER_FORMAT_SNIPPETS_HPP

#include "cucumber/query/Query.hpp"
#include <fmt/core.h>
#include <fmt/format.h>
#include <optional>
#include <string>

namespace cucumber::pretty_formatter
{
    std::optional<std::string> FormatSnippets(const query::Query& query);
}

#endif
