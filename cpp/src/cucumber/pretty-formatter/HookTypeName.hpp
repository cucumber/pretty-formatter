#ifndef CUCUMBER_PRETTY_FORMATTER_HOOK_TYPE_NAME_HPP
#define CUCUMBER_PRETTY_FORMATTER_HOOK_TYPE_NAME_HPP

#include "cucumber/messages/HookType.hpp"
#include <optional>
#include <string_view>

namespace cucumber::pretty_formatter
{
    [[nodiscard]] std::string_view HookTypeName(const std::optional<messages::HookType>& type);
}

#endif
