#include "cucumber/pretty-formatter/HookTypeName.hpp"
#include "cucumber/messages/HookType.hpp"
#include <optional>
#include <string_view>
#include <unordered_map>

namespace cucumber::pretty_formatter
{
    namespace
    {
        const std::unordered_map<messages::HookType, std::string_view> hookTypeNames = {
            { messages::HookType::BEFORE_TEST_RUN, "BeforeAll" },
            { messages::HookType::AFTER_TEST_RUN, "AfterAll" },
            { messages::HookType::BEFORE_TEST_CASE, "Before" },
            { messages::HookType::AFTER_TEST_CASE, "After" },
            { messages::HookType::BEFORE_TEST_STEP, "BeforeStep" },
            { messages::HookType::AFTER_TEST_STEP, "AfterStep" },
        };
    }

    std::string_view HookTypeName(const std::optional<messages::HookType>& type)
    {
        if (!type.has_value())
        {
            return "Unknown";
        }
        return hookTypeNames.at(type.value());
    }
}
