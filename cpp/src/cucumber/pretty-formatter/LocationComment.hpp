#ifndef CUCUMBER_PRETTY_FORMATTER_LOCATION_COMMENT_HPP
#define CUCUMBER_PRETTY_FORMATTER_LOCATION_COMMENT_HPP

#include "cucumber/messages/SourceReference.hpp"
#include "cucumber/pretty-formatter/LineBuilder.hpp"
#include "cucumber/pretty-formatter/SourceReferenceFormatter.hpp"
#include <memory>
#include <optional>
#include <string>

namespace cucumber::pretty_formatter
{
    void AppendLocationComment(LineBuilder& lineBuilder, const std::string& comment);

    void AppendLocationComment(LineBuilder& lineBuilder, const std::optional<std::string>& comment);

    void AppendLocationComment(LineBuilder& lineBuilder, const SourceReferenceFormatter& sourceReferenceFormatter,
        const std::shared_ptr<const messages::SourceReference>& sourceReference);
}

#endif
