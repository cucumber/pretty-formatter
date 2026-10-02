#ifndef CUCUMBER_PRETTY_FORMATTER_THEME_HPP
#define CUCUMBER_PRETTY_FORMATTER_THEME_HPP

#include "cucumber/messages/TestStepResultStatus.hpp"
#include "cucumber/pretty-formatter/Ansi.hpp"
#include <cstddef>
#include <cstdint>
#include <map>
#include <memory>
#include <optional>
#include <string>
#include <string_view>
#include <utility>

namespace cucumber::pretty_formatter
{
    struct Theme
    {
        static constexpr std::size_t statusIconLength = 1;

        enum class Element : std::uint8_t
        {
            attachment,
            dataTable,
            dataTableBorder,
            dataTableContent,
            docString,
            docStringContent,
            docStringMediaType,
            docStringDelimiter,
            feature,
            featureKeyword,
            featureName,
            location,
            progressIcon,
            rule,
            ruleKeyword,
            ruleName,
            scenario,
            scenarioKeyword,
            scenarioName,
            statusIcon,
            step,
            stepArgument,
            stepKeyword,
            stepText,
            tag,
        };

        Theme() = default;

        Theme(std::map<messages::TestStepResultStatus, std::string> statusIconByStatus,
            std::map<messages::TestStepResultStatus, std::string> progressIconByStatus,
            std::map<Element, std::pair<Ansi, Ansi>> styleByElement,
            std::map<Element, std::map<messages::TestStepResultStatus, std::pair<Ansi, Ansi>>> styleByStatusByElement,
            std::optional<std::string> bulletPointIcon);

        static std::unique_ptr<Theme> Cucumber();
        static std::unique_ptr<Theme> Plain();
        static std::unique_ptr<Theme> None();

        [[nodiscard]] std::string Style(Element element, std::string_view text) const;
        [[nodiscard]] std::string Style(Element element, messages::TestStepResultStatus status, std::string_view text) const;

        [[nodiscard]] std::string BeginStyle(Element element) const;
        [[nodiscard]] std::string BeginStyle(Element element, messages::TestStepResultStatus status) const;

        [[nodiscard]] std::string EndStyle(Element element) const;
        [[nodiscard]] std::string EndStyle(Element element, messages::TestStepResultStatus status) const;

        [[nodiscard]] std::string ProgressIcon(messages::TestStepResultStatus status) const;
        [[nodiscard]] std::string StatusIcon(messages::TestStepResultStatus status) const;

        [[nodiscard]] bool HasStatusIcons() const;

        [[nodiscard]] std::string BulletPointIcon() const;

        struct Factory
        {
            Factory& Style(Element element, Ansi style, Ansi resetStyle);

            Factory& Style(Element element, messages::TestStepResultStatus status, Ansi style, Ansi resetStyle);

            Factory& StatusIcon(messages::TestStepResultStatus status, std::string icon);

            Factory& ProgressIcon(messages::TestStepResultStatus status, std::string icon);

            Factory& BulletPointIcon(std::string icon);

            std::unique_ptr<Theme> Build();

        private:
            std::map<messages::TestStepResultStatus, std::string> statusIconByStatus;
            std::map<messages::TestStepResultStatus, std::string> progressIconByStatus;
            std::map<Element, std::pair<Ansi, Ansi>> styleByElement;
            std::map<Element, std::map<messages::TestStepResultStatus, std::pair<Ansi, Ansi>>> styleByStatusByElement;
            std::optional<std::string> bulletPointIcon;
        };

    private:
        [[nodiscard]] std::optional<std::pair<Ansi, Ansi>> FindAnsiBy(Element element) const;

        [[nodiscard]] std::optional<std::pair<Ansi, Ansi>> FindAnsiBy(Element element, messages::TestStepResultStatus status) const;

        std::map<messages::TestStepResultStatus, std::string> statusIconByStatus;
        std::map<messages::TestStepResultStatus, std::string> progressIconByStatus;
        std::map<Element, std::pair<Ansi, Ansi>> styleByElement;
        std::map<Element, std::map<messages::TestStepResultStatus, std::pair<Ansi, Ansi>>> styleByStatusByElement;
        std::optional<std::string> bulletPointIcon;
    };
}

#endif
