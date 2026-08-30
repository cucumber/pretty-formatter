#include "cucumber/pretty-formatter/AttachmentFormatter.hpp"
#include "cucumber/messages/Attachment.hpp"
#include "cucumber/messages/AttachmentContentEncoding.hpp"
#include "cucumber/pretty-formatter/LineBuilder.hpp"
#include "cucumber/pretty-formatter/Theme.hpp"
#include <cstddef>
#include <fmt/format.h>
#include <memory>
#include <sstream>
#include <string>

namespace cucumber::pretty_formatter
{
    AttachmentFormatter::AttachmentFormatter(std::size_t indent)
        : indent{ indent }
    {}

    void AttachmentFormatter::Format(LineBuilder& lineBuilder, const std::shared_ptr<const messages::Attachment>& attachment)
    {
        if (attachment->contentEncoding == messages::AttachmentContentEncoding::BASE64)
        {
            FormatBase64(lineBuilder, attachment);
        }
        else
        {
            FormatText(lineBuilder, attachment);
        }
    }

    void AttachmentFormatter::FormatBase64(LineBuilder& lineBuilder, const std::shared_ptr<const messages::Attachment>& attachment) const
    {
        const auto bytes = (attachment->body.size() / 4) * 3;
        const auto filename = attachment->fileName.has_value() ? attachment->fileName.value() + " " : "";

        lineBuilder.Indent(indent)
            .Append(Theme::Element::attachment, fmt::format("Embedding {}[{} {} bytes]", filename, attachment->mediaType, bytes))
            .NewLine();
    }

    void AttachmentFormatter::FormatText(LineBuilder& lineBuilder, const std::shared_ptr<const messages::Attachment>& attachment) const
    {
        std::istringstream stream{ attachment->body };
        for (std::string line; std::getline(stream, line);)
        {
            lineBuilder.Indent(indent).Append(Theme::Element::attachment, line).NewLine();
        }
    }
}
