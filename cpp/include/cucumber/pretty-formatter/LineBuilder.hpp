#ifndef CUCUMBER_PRETTY_FORMATTER_LINE_BUILDER_HPP
#define CUCUMBER_PRETTY_FORMATTER_LINE_BUILDER_HPP

#include "cucumber/messages/TestStepResultStatus.hpp"
#include "cucumber/pretty-formatter/Theme.hpp"
#include <cstddef>
#include <functional>
#include <memory>
#include <string>
#include <string_view>

namespace cucumber::pretty_formatter
{
    struct LineBuilder
    {
        LineBuilder(std::shared_ptr<Theme> theme);

        LineBuilder& Indent(std::size_t indent);
        LineBuilder& AddPaddingUpTo(std::size_t indent);

        LineBuilder& Title(Theme::Element element, const std::string& keyword, Theme::Element nameElement, const std::string& name);

        LineBuilder& StatusIcon(const std::string& icon);

        LineBuilder& NewLine();

        LineBuilder& Append(std::string_view text);
        LineBuilder& Append(Theme::Element element, std::string_view text);
        LineBuilder& Append(Theme::Element element, messages::TestStepResultStatus status, std::string_view text);

        LineBuilder& Begin(Theme::Element element);
        LineBuilder& Begin(Theme::Element element, messages::TestStepResultStatus status);

        LineBuilder& End(Theme::Element element);
        LineBuilder& End(Theme::Element element, messages::TestStepResultStatus status);

        template<class Func>
        LineBuilder& Accept(Func&& func);

        template<class TFunc, class T>
        LineBuilder& Accept(TFunc&& func, T* instance);

        [[nodiscard]] std::string Build();

    private:
        [[nodiscard]] std::string CreatePadding(std::size_t indent) const;

        static constexpr std::size_t initialCapacity = 80;

        std::shared_ptr<Theme> theme;
        std::string string;
        std::size_t unstyledLength{ 0 };
    };

    template<class Func>
    LineBuilder& LineBuilder::Accept(Func&& func)
    {
        std::forward<Func>(func)(*this);
        return *this;
    }

    template<class TFunc, class T>
    LineBuilder& LineBuilder::Accept(TFunc&& func, T* instance)
    {
        std::invoke(std::forward<TFunc>(func), instance, *this);
        return *this;
    }
}

#endif
