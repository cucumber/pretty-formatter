#include "cucumber/pretty-formatter/ExceptionFormatter.hpp"
#include "cucumber/messages/Exception.hpp"
#include "cucumber/messages/TestStepResultStatus.hpp"
#include "cucumber/pretty-formatter/LineBuilder.hpp"
#include "cucumber/pretty-formatter/Theme.hpp"
#include <cstddef>
#include <memory>
#include <optional>
#include <sstream>
#include <string>
#include <utility>

namespace cucumber::pretty_formatter
{
    ExceptionFormatter::ExceptionFormatter(std::size_t indent, std::shared_ptr<Theme> theme, messages::TestStepResultStatus status)
        : indent{ indent }
        , theme{ std::move(theme) }
        , status{ status }
    {}

    std::optional<std::string> ExceptionFormatter::Format(const std::shared_ptr<const messages::Exception>& exception,
        const std::optional<std::string>& message)
    {
        if (status == messages::TestStepResultStatus::FAILED && exception->stackTrace.has_value())
        {
            return Format(exception->stackTrace.value());
        }

        if (status == messages::TestStepResultStatus::FAILED || status == messages::TestStepResultStatus::PENDING ||
            status == messages::TestStepResultStatus::SKIPPED)
        {
            if (exception->message.has_value())
            {
                return Format(exception->message.value());
            }
            if (message.has_value())
            {
                return Format(message.value());
            }
            return std::nullopt;
        }

        return std::nullopt;
    }

    std::optional<std::string> ExceptionFormatter::Format(const std::shared_ptr<const messages::Exception>& exception)
    {
        return Format(exception, std::nullopt);
    }

    std::string ExceptionFormatter::Format(const std::string& message)
    {
        LineBuilder builder{ theme };

        std::istringstream stream{ message };
        for (std::string line; std::getline(stream, line);)
        {
            builder.Indent(indent).Append(Theme::Element::step, status, line).NewLine();
        }

        return builder.Build();
    }
}
