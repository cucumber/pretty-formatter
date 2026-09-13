#include "cucumber/pretty-formatter/FormatResultException.hpp"
#include "cucumber/messages/TestStepResult.hpp"
#include "cucumber/pretty-formatter/ExceptionFormatter.hpp"
#include "cucumber/pretty-formatter/Theme.hpp"
#include <cstddef>
#include <memory>
#include <string>

namespace cucumber::pretty_formatter
{
    std::string FormatResultException(const messages::TestStepResult& result, std::size_t indent, const std::shared_ptr<Theme>& theme)
    {
        ExceptionFormatter exceptionFormatter{ indent, theme, result.status };
        const auto& message = result.message;

        if (result.exception)
        {
            return exceptionFormatter.Format(*result.exception, message).value_or("");
        }

        if (message)
        {
            return exceptionFormatter.Format(*message);
        }

        return "";
    }
}
