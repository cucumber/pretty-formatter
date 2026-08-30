#include "cucumber/pretty-formatter/LineBuilder.hpp"
#include "cucumber/messages/TestStepResultStatus.hpp"
#include "cucumber/pretty-formatter/Theme.hpp"
#include <cstddef>
#include <memory>
#include <string>
#include <string_view>
#include <utility>

namespace cucumber::pretty_formatter
{
    LineBuilder::LineBuilder(std::shared_ptr<Theme> theme)
        : theme{ std::move(theme) }
    {
        string.reserve(initialCapacity);
    }

    LineBuilder& LineBuilder::Indent(std::size_t indent)
    {
        return Append(CreatePadding(indent));
    }

    LineBuilder& LineBuilder::AddPaddingUpTo(std::size_t indent)
    {
        return Append(CreatePadding(indent));
    }

    LineBuilder& LineBuilder::Title(Theme::Element element, const std::string& keyword, Theme::Element nameElement, const std::string& name)
    {
        return Append(element, keyword + ":").Append(" ").Append(nameElement, name);
    }

    LineBuilder& LineBuilder::StatusIcon(const std::string& icon)
    {
        unstyledLength += Theme::statusIconLength;
        string.append(icon);
        return *this;
    }

    LineBuilder& LineBuilder::NewLine()
    {
        unstyledLength = 0;
        string.append("\n");
        return *this;
    }

    LineBuilder& LineBuilder::Append(std::string_view text)
    {
        unstyledLength += text.size();
        string.append(text);
        return *this;
    }

    LineBuilder& LineBuilder::Append(Theme::Element element, std::string_view text)
    {
        unstyledLength += text.size();
        string.append(theme->Style(element, text));
        return *this;
    }

    LineBuilder& LineBuilder::Append(Theme::Element element, messages::TestStepResultStatus status, std::string_view text)
    {
        unstyledLength += text.size();
        string.append(theme->Style(element, status, text));
        return *this;
    }

    LineBuilder& LineBuilder::Begin(Theme::Element element)
    {
        string.append(theme->BeginStyle(element));
        return *this;
    }

    LineBuilder& LineBuilder::Begin(Theme::Element element, messages::TestStepResultStatus status)
    {
        string.append(theme->BeginStyle(element, status));
        return *this;
    }

    LineBuilder& LineBuilder::End(Theme::Element element)
    {
        string.append(theme->EndStyle(element));
        return *this;
    }

    LineBuilder& LineBuilder::End(Theme::Element element, messages::TestStepResultStatus status)
    {
        string.append(theme->EndStyle(element, status));
        return *this;
    }

    std::string LineBuilder::Build()
    {
        string.resize(string.size());
        return std::move(string);
    }

    std::string LineBuilder::CreatePadding(std::size_t indent) const
    {
        if (indent <= unstyledLength)
        {
            return std::string{};
        }

        const auto padding = indent - unstyledLength;
        return std::string(padding,
            ' '); // NOLINT(modernize-return-braced-init-list): explicit constructor can't be called using braced initializers.
    }
}
