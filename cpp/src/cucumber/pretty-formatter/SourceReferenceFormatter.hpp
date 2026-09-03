#ifndef CUCUMBER_PRETTY_FORMATTER_SOURCE_REFERENCE_FORMATTER_HPP
#define CUCUMBER_PRETTY_FORMATTER_SOURCE_REFERENCE_FORMATTER_HPP

#include "cucumber/messages/Location.hpp"
#include "cucumber/messages/SourceReference.hpp"
#include <functional>
#include <memory>
#include <optional>
#include <string>

namespace cucumber::pretty_formatter
{
    struct SourceReferenceFormatter
    {
        explicit SourceReferenceFormatter(std::function<std::string(std::string)> uriFormatter);

        [[nodiscard]] std::optional<std::string> Format(const std::shared_ptr<const messages::SourceReference>& sourceReference) const;
        [[nodiscard]] std::string Format(const std::string& uri,
            const std::optional<std::shared_ptr<const messages::Location>>& location) const;

    private:
        std::function<std::string(std::string)> uriFormatter;
    };
}

#endif
