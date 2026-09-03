#ifndef CUCUMBER_PRETTY_FORMATTER_ATTACHMENT_FORMATTER_HPP
#define CUCUMBER_PRETTY_FORMATTER_ATTACHMENT_FORMATTER_HPP

#include "cucumber/messages/Attachment.hpp"
#include "cucumber/pretty-formatter/LineBuilder.hpp"
#include <cstddef>
#include <memory>

namespace cucumber::pretty_formatter
{
    struct AttachmentFormatter
    {
        explicit AttachmentFormatter(std::size_t indent);

        void Format(LineBuilder& lineBuilder, const std::shared_ptr<const messages::Attachment>& attachment);

    private:
        void FormatBase64(LineBuilder& lineBuilder, const std::shared_ptr<const messages::Attachment>& attachment) const;

        void FormatText(LineBuilder& lineBuilder, const std::shared_ptr<const messages::Attachment>& attachment) const;

        std::size_t indent;
    };
}

#endif
