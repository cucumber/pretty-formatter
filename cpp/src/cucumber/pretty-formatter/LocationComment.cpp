#include "cucumber/pretty-formatter/LocationComment.hpp"
#include "cucumber/messages/SourceReference.hpp"
#include "cucumber/pretty-formatter/LineBuilder.hpp"
#include "cucumber/pretty-formatter/SourceReferenceFormatter.hpp"
#include "cucumber/pretty-formatter/Theme.hpp"
#include <memory>
#include <optional>
#include <string>

namespace cucumber::pretty_formatter
{
    void AppendLocationComment(LineBuilder& lineBuilder, const std::string& comment)
    {
        lineBuilder.Append(" ").Append(Theme::Element::location, "# " + comment);
    }

    void AppendLocationComment(LineBuilder& lineBuilder, const std::optional<std::string>& comment)
    {
        if (comment.has_value())
        {
            AppendLocationComment(lineBuilder, comment.value());
        }
    }

    void AppendLocationComment(LineBuilder& lineBuilder, const SourceReferenceFormatter& sourceReferenceFormatter,
        const std::shared_ptr<const messages::SourceReference>& sourceReference)
    {
        AppendLocationComment(lineBuilder, sourceReferenceFormatter.Format(sourceReference));
    }
}
