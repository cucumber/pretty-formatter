#include "cucumber/pretty-formatter/PickleComparator.hpp"
#include "cucumber/messages/Pickle.hpp"
#include "fmt/core.h"
#include "fmt/format.h"
#include <cstdint>
#include <memory>
#include <optional>
#include <string>

namespace cucumber::pretty_formatter
{
    std::int32_t PickleComparator(const messages::Pickle& lhs, const messages::Pickle& rhs)
    {
        if (lhs.uri != rhs.uri)
        {
            return static_cast<std::int32_t>(rhs.uri.compare(lhs.uri));
        }
        if (!lhs.location || !rhs.location)
        {
            return 0;
        }
        if (lhs.location->line != rhs.location->line)
        {
            return static_cast<std::int32_t>(lhs.location->line) - static_cast<std::int32_t>(rhs.location->line);
        }
        return static_cast<std::int32_t>(lhs.location->column.value_or(0)) - static_cast<std::int32_t>(rhs.location->column.value_or(0));
    }
}
