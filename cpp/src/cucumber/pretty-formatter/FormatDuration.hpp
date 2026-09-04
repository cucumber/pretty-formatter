#ifndef CUCUMBER_PRETTY_FORMATTER_FORMAT_DURATION_HPP
#define CUCUMBER_PRETTY_FORMATTER_FORMAT_DURATION_HPP

#include "cucumber/messages/Duration.hpp"
#include <chrono>
#include <fmt/core.h>
#include <memory>
#include <string>

namespace cucumber::pretty_formatter
{
    std::string FormatDuration(const std::shared_ptr<const messages::Duration>& duration);
}

#endif
