#ifndef CUCUMBER_PRETTY_FORMATTER_FORMAT_RESULT_EXCEPTION_HPP
#define CUCUMBER_PRETTY_FORMATTER_FORMAT_RESULT_EXCEPTION_HPP

#include "cucumber/messages/TestStepResult.hpp"
#include "cucumber/pretty-formatter/Theme.hpp"
#include <cstddef>
#include <memory>
#include <string>

namespace cucumber::pretty_formatter
{
    [[nodiscard]] std::string FormatResultException(const std::shared_ptr<const messages::TestStepResult>& result, std::size_t indent,
        const std::shared_ptr<Theme>& theme);
}

#endif
