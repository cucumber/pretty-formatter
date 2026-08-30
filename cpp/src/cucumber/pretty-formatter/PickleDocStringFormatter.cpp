#include "cucumber/pretty-formatter/PickleDocStringFormatter.hpp"
#include "cucumber/messages/PickleDocString.hpp"
#include "cucumber/pretty-formatter/LineBuilder.hpp"
#include "cucumber/pretty-formatter/Theme.hpp"
#include <cstddef>
#include <memory>
#include <sstream>
#include <string>

namespace cucumber::pretty_formatter
{
    PickleDocStringFormatter::PickleDocStringFormatter(std::size_t indent)
        : indent{ indent }
    {}

    void PickleDocStringFormatter::Format(LineBuilder& lineBuilder, const std::shared_ptr<const messages::PickleDocString>& docString)
    {
        lineBuilder.Indent(indent)
            .Begin(Theme::Element::docString)
            .Append(Theme::Element::docStringDelimiter, delimiter)
            .Accept(
                [&docString](auto& lineBuilder)
                {
                    if (docString->mediaType.has_value())
                    {
                        lineBuilder.Append(Theme::Element::docStringMediaType, docString->mediaType.value());
                    }
                })
            .End(Theme::Element::docString)
            .NewLine();

        std::istringstream stream{ docString->content };
        for (std::string line; std::getline(stream, line);)
        {
            lineBuilder.Indent(indent)
                .Begin(Theme::Element::docString)
                .Append(Theme::Element::docStringContent, line)
                .End(Theme::Element::docString)
                .NewLine();
        }

        lineBuilder.Indent(indent)
            .Begin(Theme::Element::docString)
            .Append(Theme::Element::docStringDelimiter, delimiter)
            .End(Theme::Element::docString)
            .NewLine();
    }
}
