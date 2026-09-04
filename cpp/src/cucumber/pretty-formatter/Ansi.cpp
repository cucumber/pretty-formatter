#include "cucumber/pretty-formatter/Ansi.hpp"
#include <cstdint>
#include <initializer_list>
#include <string>

namespace cucumber::pretty_formatter
{
    namespace
    {
        constexpr char firstEscape = 27;
        constexpr char secondEscape = '[';
        constexpr char endSequence = 'm';

        std::string BuildSequence(std::initializer_list<Ansi::Attribute> attributes)
        {
            std::string str;
            str.reserve(attributes.size() * (3 + 2)); // 3 escape characters + 2 digits for the attribute value

            for (const auto& attribute : attributes)
            {
                str += firstEscape;
                str += secondEscape;
                str += std::to_string(static_cast<std::uint8_t>(attribute));
                str += endSequence;
            }

            return str;
        }
    }

    Ansi::Ansi(std::initializer_list<Attribute> attributes)
        : controlSequence{ BuildSequence(attributes) }
    {}

    std::string Ansi::ToString() const
    {
        return controlSequence;
    }
}
