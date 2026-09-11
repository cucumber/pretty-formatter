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

    std::optional<std::string> SourceReferenceFormatter::Format(const messages::SourceReference& sourceReference) const
    {
        if (sourceReference.uri)
        {
            auto uri = uriFormatter(*sourceReference.uri);
            if (sourceReference.location)
            {
                return uri + ":" + std::to_string(sourceReference.location->line);
            }
            return uri;
        }

        return std::nullopt;
    }

    std::string SourceReferenceFormatter::Format(const std::string& uri, const messages::Location* location) const
    {
        auto uriFormatted = uriFormatter(uri);

        if (location != nullptr)
        {
            return uriFormatted + ":" + std::to_string(location->line);
        }

        return uriFormatted;
    }
}
