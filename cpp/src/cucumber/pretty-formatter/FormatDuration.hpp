#ifndef CUCUMBER_PRETTY_FORMATTER_FORMAT_DURATION_HPP
#define CUCUMBER_PRETTY_FORMATTER_FORMAT_DURATION_HPP

#include "cucumber/messages/Duration.hpp"
#include "fmt/core.h"
#include <chrono>
#include <memory>
#include <string>

namespace cucumber::pretty_formatter
{
    std::string FormatDuration(const messages::Duration& duration);
}

#endif
