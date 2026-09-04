#ifndef CUCUMBER_PRETTY_FORMATTER_PICKLE_COMPARATOR_HPP
#define CUCUMBER_PRETTY_FORMATTER_PICKLE_COMPARATOR_HPP

#include "cucumber/messages/Pickle.hpp"
#include <cstdint>
#include <fmt/core.h>
#include <fmt/format.h>
#include <memory>

namespace cucumber::pretty_formatter
{
    std::int32_t PickleComparator(const std::shared_ptr<const messages::Pickle>& lhs, const std::shared_ptr<const messages::Pickle>& rhs);
}

#endif
