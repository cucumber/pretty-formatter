#ifndef CUCUMBER_PRETTY_FORMATTER_EXCEPTION_FORMATTER_HPP
#define CUCUMBER_PRETTY_FORMATTER_EXCEPTION_FORMATTER_HPP

#include "cucumber/messages/Exception.hpp"
#include "cucumber/messages/TestStepResultStatus.hpp"
#include "cucumber/pretty-formatter/Theme.hpp"
#include <cstddef>
#include <memory>
#include <optional>
#include <string>

namespace cucumber::pretty_formatter
{
    struct ExceptionFormatter
    {
        ExceptionFormatter(std::size_t indent, std::shared_ptr<Theme> theme, messages::TestStepResultStatus status);

        std::optional<std::string> Format(const std::shared_ptr<const messages::Exception>& exception, std::optional<std::string> message);

        std::optional<std::string> Format(const std::shared_ptr<const messages::Exception>& exception);

        std::string Format(const std::string& message);

    private:
        std::size_t indent;
        std::shared_ptr<Theme> theme;
        messages::TestStepResultStatus status;
    };
}

#endif
