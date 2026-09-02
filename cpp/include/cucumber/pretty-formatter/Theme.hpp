#ifndef CUCUMBER_PRETTY_FORMATTER_THEME_HPP
#define CUCUMBER_PRETTY_FORMATTER_THEME_HPP

#include "cucumber/messages/TestStepResultStatus.hpp"
#include "cucumber/pretty-formatter/Ansi.hpp"
#include <cstddef>
#include <cstdint>
#include <fmt/core.h>
#include <fmt/format.h>
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
            std::optional<std::string> bulletPointIcon)
            : statusIconByStatus{ std::move(statusIconByStatus) }
            , progressIconByStatus{ std::move(progressIconByStatus) }
            , styleByElement{ std::move(styleByElement) }
            , styleByStatusByElement{ std::move(styleByStatusByElement) }
            , bulletPointIcon{ std::move(bulletPointIcon) }
        {}

        static std::unique_ptr<Theme> Cucumber()
        {
            return Factory{}
                .Style(Element::attachment, Ansi{ Ansi::Attribute::foregroundBlue }, Ansi{ Ansi::Attribute::foregroundDefault })
                .Style(Element::featureKeyword, Ansi{ Ansi::Attribute::bold }, Ansi{ Ansi::Attribute::boldOff })
                .Style(Element::location, Ansi{ Ansi::Attribute::foregroundBrightBlack }, Ansi{ Ansi::Attribute::foregroundDefault })
                .Style(Element::ruleKeyword, Ansi{ Ansi::Attribute::bold }, Ansi{ Ansi::Attribute::boldOff })
                .Style(Element::scenarioKeyword, Ansi{ Ansi::Attribute::bold }, Ansi{ Ansi::Attribute::boldOff })
                .Style(Element::step, messages::TestStepResultStatus::AMBIGUOUS, Ansi{ Ansi::Attribute::foregroundMagenta },
                    Ansi{ Ansi::Attribute::foregroundDefault })
                .Style(Element::step, messages::TestStepResultStatus::FAILED, Ansi{ Ansi::Attribute::foregroundRed },
                    Ansi{ Ansi::Attribute::foregroundDefault })
                .Style(Element::step, messages::TestStepResultStatus::PASSED, Ansi{ Ansi::Attribute::foregroundGreen },
                    Ansi{ Ansi::Attribute::foregroundDefault })
                .Style(Element::step, messages::TestStepResultStatus::PENDING, Ansi{ Ansi::Attribute::foregroundCyan },
                    Ansi{ Ansi::Attribute::foregroundDefault })
                .Style(Element::step, messages::TestStepResultStatus::SKIPPED, Ansi{ Ansi::Attribute::foregroundYellow },
                    Ansi{ Ansi::Attribute::foregroundDefault })
                .Style(Element::step, messages::TestStepResultStatus::UNDEFINED, Ansi{ Ansi::Attribute::foregroundBlue },
                    Ansi{ Ansi::Attribute::foregroundDefault })
                .Style(Element::stepArgument, Ansi{ Ansi::Attribute::bold }, Ansi{ Ansi::Attribute::boldOff })
                .Style(Element::stepKeyword, Ansi{ Ansi::Attribute::bold }, Ansi{ Ansi::Attribute::boldOff })
                .StatusIcon(messages::TestStepResultStatus::AMBIGUOUS, "✘")
                .Style(Element::statusIcon, messages::TestStepResultStatus::AMBIGUOUS, Ansi{ Ansi::Attribute::foregroundMagenta },
                    Ansi{ Ansi::Attribute::foregroundDefault })
                .StatusIcon(messages::TestStepResultStatus::FAILED, "✘")
                .Style(Element::statusIcon, messages::TestStepResultStatus::FAILED, Ansi{ Ansi::Attribute::foregroundRed },
                    Ansi{ Ansi::Attribute::foregroundDefault })
                .StatusIcon(messages::TestStepResultStatus::PASSED, "✔")
                .Style(Element::statusIcon, messages::TestStepResultStatus::PASSED, Ansi{ Ansi::Attribute::foregroundGreen },
                    Ansi{ Ansi::Attribute::foregroundDefault })
                .StatusIcon(messages::TestStepResultStatus::PENDING, "■")
                .Style(Element::statusIcon, messages::TestStepResultStatus::PENDING, Ansi{ Ansi::Attribute::foregroundCyan },
                    Ansi{ Ansi::Attribute::foregroundDefault })
                .StatusIcon(messages::TestStepResultStatus::SKIPPED, "↷")
                .Style(Element::statusIcon, messages::TestStepResultStatus::SKIPPED, Ansi{ Ansi::Attribute::foregroundYellow },
                    Ansi{ Ansi::Attribute::foregroundDefault })
                .StatusIcon(messages::TestStepResultStatus::UNDEFINED, "■")
                .Style(Element::statusIcon, messages::TestStepResultStatus::UNDEFINED, Ansi{ Ansi::Attribute::foregroundBlue },
                    Ansi{ Ansi::Attribute::foregroundDefault })
                .ProgressIcon(messages::TestStepResultStatus::AMBIGUOUS, "A")
                .Style(Element::progressIcon, messages::TestStepResultStatus::AMBIGUOUS, Ansi{ Ansi::Attribute::foregroundMagenta },
                    Ansi{ Ansi::Attribute::foregroundDefault })
                .ProgressIcon(messages::TestStepResultStatus::FAILED, "F")
                .Style(Element::progressIcon, messages::TestStepResultStatus::FAILED, Ansi{ Ansi::Attribute::foregroundRed },
                    Ansi{ Ansi::Attribute::foregroundDefault })
                .ProgressIcon(messages::TestStepResultStatus::PASSED, ".")
                .Style(Element::progressIcon, messages::TestStepResultStatus::PASSED, Ansi{ Ansi::Attribute::foregroundGreen },
                    Ansi{ Ansi::Attribute::foregroundDefault })
                .ProgressIcon(messages::TestStepResultStatus::PENDING, "P")
                .Style(Element::progressIcon, messages::TestStepResultStatus::PENDING, Ansi{ Ansi::Attribute::foregroundCyan },
                    Ansi{ Ansi::Attribute::foregroundDefault })
                .ProgressIcon(messages::TestStepResultStatus::SKIPPED, "-")
                .Style(Element::progressIcon, messages::TestStepResultStatus::SKIPPED, Ansi{ Ansi::Attribute::foregroundYellow },
                    Ansi{ Ansi::Attribute::foregroundDefault })
                .ProgressIcon(messages::TestStepResultStatus::UNDEFINED, "U")
                .Style(Element::progressIcon, messages::TestStepResultStatus::UNDEFINED, Ansi{ Ansi::Attribute::foregroundBlue },
                    Ansi{ Ansi::Attribute::foregroundDefault })
                .BulletPointIcon("•")
                .Build();
        }

        static std::unique_ptr<Theme> Plain()
        {
            return Factory{}
                .StatusIcon(messages::TestStepResultStatus::AMBIGUOUS, "✘")
                .StatusIcon(messages::TestStepResultStatus::FAILED, "✘")
                .StatusIcon(messages::TestStepResultStatus::PASSED, "✔")
                .StatusIcon(messages::TestStepResultStatus::PENDING, "■")
                .StatusIcon(messages::TestStepResultStatus::SKIPPED, "↷")
                .StatusIcon(messages::TestStepResultStatus::UNDEFINED, "■")
                .ProgressIcon(messages::TestStepResultStatus::AMBIGUOUS, "A")
                .ProgressIcon(messages::TestStepResultStatus::FAILED, "F")
                .ProgressIcon(messages::TestStepResultStatus::PASSED, ".")
                .ProgressIcon(messages::TestStepResultStatus::PENDING, "P")
                .ProgressIcon(messages::TestStepResultStatus::SKIPPED, "-")
                .ProgressIcon(messages::TestStepResultStatus::UNDEFINED, "U")
                .BulletPointIcon("-")
                .Build();
        }

        static std::unique_ptr<Theme> None()
        {
            return Factory{}.Build();
        }

        [[nodiscard]] std::string Style(Element element, std::string_view text) const
        {
            const auto ansi = FindAnsiBy(element);
            if (ansi.has_value())
            {
                return fmt::format("{}{}{}", ansi->first.ToString(), text, ansi->second.ToString());
            }
            return std::string{ text };
        }

        [[nodiscard]] std::string Style(Element element, messages::TestStepResultStatus status, std::string_view text) const
        {
            const auto ansi = FindAnsiBy(element, status);
            if (ansi.has_value())
            {
                return fmt::format("{}{}{}", ansi->first.ToString(), text, ansi->second.ToString());
            }
            return std::string{ text };
        }

        [[nodiscard]] std::string BeginStyle(Element element) const
        {
            const auto ansi = FindAnsiBy(element);
            if (ansi.has_value())
            {
                return ansi->first.ToString();
            }
            return "";
        }

        [[nodiscard]] std::string BeginStyle(Element element, messages::TestStepResultStatus status) const
        {
            const auto ansi = FindAnsiBy(element, status);
            if (ansi.has_value())
            {
                return ansi->first.ToString();
            }
            return "";
        }

        [[nodiscard]] std::string EndStyle(Element element) const
        {
            const auto ansi = FindAnsiBy(element);
            if (ansi.has_value())
            {
                return ansi->second.ToString();
            }
            return "";
        }

        [[nodiscard]] std::string EndStyle(Element element, messages::TestStepResultStatus status) const
        {
            const auto ansi = FindAnsiBy(element, status);
            if (ansi.has_value())
            {
                return ansi->second.ToString();
            }
            return "";
        }

        [[nodiscard]] std::string ProgressIcon(messages::TestStepResultStatus status) const
        {
            if (progressIconByStatus.find(status) != progressIconByStatus.end())
            {
                return progressIconByStatus.at(status);
            }
            return " ";
        }

        [[nodiscard]] std::string StatusIcon(messages::TestStepResultStatus status) const
        {
            if (statusIconByStatus.find(status) != statusIconByStatus.end())
            {
                return statusIconByStatus.at(status);
            }
            return " ";
        }

        [[nodiscard]] bool HasStatusIcons() const
        {
            return !statusIconByStatus.empty();
        }

        [[nodiscard]] std::string BulletPointIcon() const
        {
            return bulletPointIcon.value_or(" ");
        }

        struct Factory
        {
            Factory& Style(Element element, Ansi style, Ansi resetStyle)
            {
                styleByElement[element] = { std::move(style), std::move(resetStyle) };
                return *this;
            }

            Factory& Style(Element element, messages::TestStepResultStatus status, Ansi style, Ansi resetStyle)
            {
                styleByStatusByElement[element][status] = { std::move(style), std::move(resetStyle) };
                return *this;
            }

            Factory& StatusIcon(messages::TestStepResultStatus status, std::string icon)
            {
                statusIconByStatus[status] = std::move(icon);
                return *this;
            }

            Factory& ProgressIcon(messages::TestStepResultStatus status, std::string icon)
            {
                progressIconByStatus[status] = std::move(icon);
                return *this;
            }

            Factory& BulletPointIcon(std::string icon)
            {
                bulletPointIcon = std::move(icon);
                return *this;
            }

            std::unique_ptr<Theme> Build()
            {
                return std::make_unique<Theme>(statusIconByStatus, progressIconByStatus, styleByElement, styleByStatusByElement,
                    bulletPointIcon);
            }

        private:
            std::map<messages::TestStepResultStatus, std::string> statusIconByStatus;
            std::map<messages::TestStepResultStatus, std::string> progressIconByStatus;
            std::map<Element, std::pair<Ansi, Ansi>> styleByElement;
            std::map<Element, std::map<messages::TestStepResultStatus, std::pair<Ansi, Ansi>>> styleByStatusByElement;
            std::optional<std::string> bulletPointIcon;
        };

    private:
        [[nodiscard]] std::optional<std::pair<Ansi, Ansi>> FindAnsiBy(Element element) const
        {
            if (styleByElement.find(element) != styleByElement.end())
            {
                return styleByElement.at(element);
            }
            return std::nullopt;
        }

        [[nodiscard]] std::optional<std::pair<Ansi, Ansi>> FindAnsiBy(Element element, messages::TestStepResultStatus status) const
        {
            if (styleByStatusByElement.find(element) != styleByStatusByElement.end())
            {
                const auto styleByStatus = styleByStatusByElement.at(element);

                if (styleByStatus.find(status) != styleByStatus.end())
                {
                    return styleByStatus.at(status);
                }
            }

            return std::nullopt;
        }

        std::map<messages::TestStepResultStatus, std::string> statusIconByStatus;
        std::map<messages::TestStepResultStatus, std::string> progressIconByStatus;
        std::map<Element, std::pair<Ansi, Ansi>> styleByElement;
        std::map<Element, std::map<messages::TestStepResultStatus, std::pair<Ansi, Ansi>>> styleByStatusByElement;
        std::optional<std::string> bulletPointIcon;
    };
}

#endif
