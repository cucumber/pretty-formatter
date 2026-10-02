#include "cucumber/pretty-formatter/FormatDuration.hpp"
#include "cucumber/messages/Duration.hpp"
#include "fmt/core.h"
#include "fmt/format.h"
#include <chrono>
#include <memory>
#include <string>

namespace cucumber::pretty_formatter
{
    namespace
    {
        std::chrono::minutes ToMinutes(const messages::Duration& duration)
        {
            return std::chrono::minutes{ duration.seconds / 60 };
        }

        std::chrono::seconds ToSeconds(const messages::Duration& duration)
        {
            return std::chrono::seconds{ duration.seconds % 60 };
        }

        std::chrono::milliseconds ToMilliSeconds(const messages::Duration& duration)
        {
            return std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::nanoseconds{ duration.nanos });
        }
    }

    std::string FormatDuration(const messages::Duration& duration)
    {
        return fmt::format("{}m {}.{:03}s", ToMinutes(duration).count(), ToSeconds(duration).count(), ToMilliSeconds(duration).count());
    }
}
