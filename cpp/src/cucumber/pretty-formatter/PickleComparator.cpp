#include "cucumber/pretty-formatter/PickleComparator.hpp"
#include "cucumber/messages/Pickle.hpp"
#include <cstdint>
#include <fmt/core.h>
#include <fmt/format.h>
#include <memory>
#include <optional>
#include <string>

namespace cucumber::pretty_formatter
{
    std::int32_t PickleComparator(const std::shared_ptr<const messages::Pickle>& lhs, const std::shared_ptr<const messages::Pickle>& rhs)
    {
        if (lhs->uri != rhs->uri)
        {
            return static_cast<std::int32_t>(rhs->uri.compare(lhs->uri));
        }
        if (!lhs->location.has_value() || !rhs->location.has_value())
        {
            return 0;
        }
        if (lhs->location.value()->line != rhs->location.value()->line)
        {
            return static_cast<std::int32_t>(lhs->location.value()->line) - static_cast<std::int32_t>(rhs->location.value()->line);
        }
        return static_cast<std::int32_t>(lhs->location.value()->column.value_or(0)) -
               static_cast<std::int32_t>(rhs->location.value()->column.value_or(0));
    }
}
