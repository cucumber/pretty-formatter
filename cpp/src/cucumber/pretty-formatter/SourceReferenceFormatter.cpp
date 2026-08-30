#include "cucumber/pretty-formatter/SourceReferenceFormatter.hpp"
#include "cucumber/messages/Location.hpp"
#include "cucumber/messages/SourceReference.hpp"
#include <functional>
#include <memory>
#include <optional>
#include <string>
#include <utility>

namespace cucumber::pretty_formatter
{
    SourceReferenceFormatter::SourceReferenceFormatter(std::function<std::string(std::string)> uriFormatter)
        : uriFormatter{ std::move(uriFormatter) }
    {}

    std::optional<std::string> SourceReferenceFormatter::Format(
        const std::shared_ptr<const messages::SourceReference>& sourceReference) const
    {
        if (sourceReference->uri.has_value())
        {
            auto uri = uriFormatter(sourceReference->uri.value());
            if (sourceReference->location.has_value())
            {
                return uri + ":" + std::to_string(sourceReference->location.value()->line);
            }
            return uri;
        }

        return std::nullopt;
    }

    std::string SourceReferenceFormatter::Format(const std::string& uri,
        const std::optional<std::shared_ptr<const messages::Location>>& location) const
    {
        auto uriFormatted = uriFormatter(uri);

        if (location.has_value())
        {
            return uriFormatted + ":" + std::to_string(location.value()->line);
        }

        return uriFormatted;
    }
}
