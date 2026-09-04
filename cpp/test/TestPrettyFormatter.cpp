#include "EnumerateTestData.hpp"
#include "cucumber/messages/Envelope.hpp"
#include "cucumber/messages/TestStepResultStatus.hpp"
#include "cucumber/pretty-formatter/Ansi.hpp"
#include "cucumber/pretty-formatter/Formatter.hpp"
#include "cucumber/pretty-formatter/PrettyPrinter.hpp"
#include "cucumber/pretty-formatter/ProgressBarPrinter.hpp"
#include "cucumber/pretty-formatter/ProgressPrinter.hpp"
#include "cucumber/pretty-formatter/SummaryPrinter.hpp"
#include "cucumber/pretty-formatter/Theme.hpp"
#include "nlohmann/json.hpp"
#include "nlohmann/json_fwd.hpp"
#include "gmock/gmock.h"
#include "gtest/gtest.h"
#include <cctype>
#include <cstddef>
#include <cstdint>
#include <filesystem>
#include <fmt/core.h>
#include <fmt/format.h>
#include <fmt/ostream.h>
#include <fstream>
#include <iterator>
#include <map>
#include <memory>
#include <sstream>
#include <string>
#include <string_view>
#include <utility>
#include <vector>

namespace cucumber::pretty_formatter
{
    namespace
    {
        struct FakeTty : ProgressBarPrinter::Tty
        {
            void Write(std::string_view output) override
            {
                content += output;
                cursorLinesUp = 0;
            }

            void ClearScreenDown() override
            {
                if (cursorLinesUp > 0)
                {
                    auto pos = content.size();
                    for (std::size_t i = 0; i < cursorLinesUp; ++i)
                    {
                        if (pos == 0)
                        {
                            content.clear();
                            break;
                        }
                        pos = content.rfind('\n', pos - 1);
                        if (pos == std::string::npos)
                        {
                            content.clear();
                            break;
                        }
                    }

                    if (pos != std::string::npos)
                    {
                        content.erase(pos);
                    }
                }
                cursorLinesUp = 0;
            }

            void MoveCursorUp(std::size_t lines) override
            {
                cursorLinesUp += lines;
            }

            [[nodiscard]] std::string GetOutput() const
            {
                return content;
            }

        private:
            std::string content;
            std::size_t cursorLinesUp{ 0 };
        };

        std::string_view GetMessageType(const messages::Envelope& envelope)
        {
            if (envelope.undefinedParameterType.has_value())
            {
                return "undefinedParameterType";
            }
            if (envelope.testRunStarted.has_value())
            {
                return "testRunStarted";
            }
            if (envelope.testCase.has_value())
            {
                return "testCase";
            }
            if (envelope.testRunHookFinished.has_value())
            {
                return "testRunHookFinished";
            }
            if (envelope.testCaseStarted.has_value())
            {
                return "testCaseStarted";
            }
            if (envelope.testStepFinished.has_value())
            {
                return "testStepFinished";
            }
            if (envelope.testCaseFinished.has_value())
            {
                return "testCaseFinished";
            }
            if (envelope.testRunFinished.has_value())
            {
                return "testRunFinished";
            }
            return "unknown";
        }

        std::shared_ptr<Theme> Demo()
        {
            return Theme::Factory{}
                .Style(Theme::Element::attachment, Ansi{ Ansi::Attribute::foregroundBlue }, Ansi{ Ansi::Attribute::foregroundDefault })
                .Style(Theme::Element::dataTable, Ansi{ Ansi::Attribute::foregroundBrightBlack },
                    Ansi{ Ansi::Attribute::foregroundDefault })
                .Style(Theme::Element::dataTableBorder, Ansi{ Ansi::Attribute::faint }, Ansi{ Ansi::Attribute::faintOff })
                .Style(Theme::Element::dataTableContent, Ansi{ Ansi::Attribute::italic }, Ansi{ Ansi::Attribute::italicOff })
                .Style(Theme::Element::docString, Ansi{ Ansi::Attribute::foregroundBrightBlack },
                    Ansi{ Ansi::Attribute::foregroundDefault })
                .Style(Theme::Element::docStringContent, Ansi{ Ansi::Attribute::italic }, Ansi{ Ansi::Attribute::italicOff })
                .Style(Theme::Element::docStringDelimiter, Ansi{ Ansi::Attribute::faint }, Ansi{ Ansi::Attribute::faintOff })
                .Style(Theme::Element::docStringMediaType, Ansi{ Ansi::Attribute::bold }, Ansi{ Ansi::Attribute::boldOff })
                .Style(Theme::Element::feature, Ansi{ Ansi::Attribute::backgroundBlue }, Ansi{ Ansi::Attribute::backgroundDefault })
                .Style(Theme::Element::featureKeyword, Ansi{ Ansi::Attribute::bold }, Ansi{ Ansi::Attribute::boldOff })
                .Style(Theme::Element::featureName, Ansi{ Ansi::Attribute::italic }, Ansi{ Ansi::Attribute::italicOff })
                .Style(Theme::Element::location, Ansi{ Ansi::Attribute::foregroundBrightBlack }, Ansi{ Ansi::Attribute::foregroundDefault })
                .Style(Theme::Element::rule, Ansi{ Ansi::Attribute::backgroundBlue }, Ansi{ Ansi::Attribute::backgroundDefault })
                .Style(Theme::Element::ruleKeyword, Ansi{ Ansi::Attribute::bold }, Ansi{ Ansi::Attribute::boldOff })
                .Style(Theme::Element::ruleName, Ansi{ Ansi::Attribute::italic }, Ansi{ Ansi::Attribute::italicOff })
                .Style(Theme::Element::scenario, Ansi{ Ansi::Attribute::backgroundBlue }, Ansi{ Ansi::Attribute::backgroundDefault })
                .Style(Theme::Element::scenarioKeyword, Ansi{ Ansi::Attribute::bold }, Ansi{ Ansi::Attribute::boldOff })
                .Style(Theme::Element::scenarioName, Ansi{ Ansi::Attribute::italic }, Ansi{ Ansi::Attribute::italicOff })
                .Style(Theme::Element::step, messages::TestStepResultStatus::UNDEFINED, Ansi{ Ansi::Attribute::foregroundBlue },
                    Ansi{ Ansi::Attribute::foregroundDefault })
                .Style(Theme::Element::step, messages::TestStepResultStatus::PENDING, Ansi{ Ansi::Attribute::foregroundCyan },
                    Ansi{ Ansi::Attribute::foregroundDefault })
                .Style(Theme::Element::step, messages::TestStepResultStatus::FAILED, Ansi{ Ansi::Attribute::foregroundRed },
                    Ansi{ Ansi::Attribute::foregroundDefault })
                .Style(Theme::Element::step, messages::TestStepResultStatus::AMBIGUOUS, Ansi{ Ansi::Attribute::foregroundMagenta },
                    Ansi{ Ansi::Attribute::foregroundDefault })
                .Style(Theme::Element::step, messages::TestStepResultStatus::PASSED, Ansi{ Ansi::Attribute::foregroundGreen },
                    Ansi{ Ansi::Attribute::foregroundDefault })
                .Style(Theme::Element::step, messages::TestStepResultStatus::SKIPPED, Ansi{ Ansi::Attribute::foregroundYellow },
                    Ansi{ Ansi::Attribute::foregroundDefault })
                .Style(Theme::Element::stepArgument, Ansi{ Ansi::Attribute::bold }, Ansi{ Ansi::Attribute::boldOff })
                .Style(Theme::Element::stepKeyword, Ansi{ Ansi::Attribute::bold }, Ansi{ Ansi::Attribute::boldOff })
                .Style(Theme::Element::stepText, Ansi{ Ansi::Attribute::italic }, Ansi{ Ansi::Attribute::italicOff })
                .Style(Theme::Element::tag, Ansi{ Ansi::Attribute::foregroundYellow, Ansi::Attribute::bold },
                    Ansi{ Ansi::Attribute::boldOff, Ansi::Attribute::foregroundDefault })
                .BulletPointIcon("•")
                .Build();
        }

        std::string Sanitize(std::string_view text)
        {
            std::string result;
            for (const char c : text)
            {
                result += std::isalnum(static_cast<unsigned char>(c)) ? c : '_';
            }
            return result.empty() ? std::string{ "empty" } : result;
        }

        struct PrettyFormatterBaseTest : public testing::TestWithParam<FormatterTestParam>
        {
            void Validate(std::filesystem::path input, std::filesystem::path output, std::unique_ptr<Formatter> formatter)
            {
                std::ifstream inputStream{ input };
                for (std::string line; std::getline(inputStream, line);)
                {
                    formatter->Update(nlohmann::json::parse(line));
                }

                std::ifstream expected{ output };
                EXPECT_THAT(stream.str(),
                    testing::StrEq(std::string{ (std::istreambuf_iterator<char>(expected)), std::istreambuf_iterator<char>() }));
            }

        protected:
            std::ostringstream stream;
        };

        struct PrettyFormatterTest : PrettyFormatterBaseTest
        {};

        struct SummaryFormatterTest : PrettyFormatterBaseTest
        {};

        struct ProgressFormatterTest : PrettyFormatterBaseTest
        {};

        struct ProgressBarFormatterTest : public testing::TestWithParam<FormatterTestParam>
        {

            void Validate(std::filesystem::path input, std::filesystem::path output, std::unique_ptr<Formatter> formatter, FakeTty& fakeTty)
            {
                std::ostringstream stream;

                std::ifstream inputStream{ input };

                std::string previousContent;
                for (std::string line; std::getline(inputStream, line);)
                {
                    messages::Envelope envelope;
                    envelope.from_json(nlohmann::json::parse(line));

                    formatter->Update(envelope);

                    auto newContent = fakeTty.GetOutput();

                    if (!newContent.empty() && newContent != previousContent)
                    {
                        if (stream.tellp() > 0)
                        {
                            stream << "\n";
                        }
                        fmt::print(stream, "[{}]\n{}", GetMessageType(envelope), Indent(newContent, 2));
                        previousContent = std::move(newContent);
                    }
                }

                std::ifstream expected{ output };
                EXPECT_THAT(stream.str(),
                    testing::StrEq(std::string{ (std::istreambuf_iterator<char>(expected)), std::istreambuf_iterator<char>() }));
            }

        private:
            std::string Indent(const std::string& text, std::size_t indent)
            {
                std::istringstream stream{ text };
                std::string indented;

                for (std::string line; std::getline(stream, line);)
                {
                    indented += std::string(indent, ' ') + line + "\n";
                }

                if (text.back() == '\n')
                {
                    indented += std::string(indent, ' ');
                }

                return indented;
            }
        };
    }

    TEST_P(PrettyFormatterTest, TestData)
    {
        std::map<std::string_view, std::shared_ptr<Theme>> themes{
            { "cucumber", Theme::Cucumber() },
            { "demo", Demo() },
            { "plain", Theme::Plain() },
            { "none", Theme::None() },
            { "exclude-features-and-rules", Theme::None() },
            { "exclude-attachments", Theme::None() },
        };

        ASSERT_THAT(themes, testing::Contains(testing::Key(GetParam().theme)));

        Validate(GetParam().input, GetParam().output,
            std::move(PrettyPrinter::Factory{}
                    .Theme(themes.at(GetParam().theme))
                    .Options(PrettyPrinter::Options::includeFeatureLine, GetParam().theme != "exclude-features-and-rules")
                    .Options(PrettyPrinter::Options::includeRuleLine, GetParam().theme != "exclude-features-and-rules")
                    .Options(PrettyPrinter::Options::includeAttachments, GetParam().theme != "exclude-attachments")
                    .Build(stream)));
    }

    TEST_P(SummaryFormatterTest, TestData)
    {
        std::map<std::string_view, std::shared_ptr<Theme>> themes{
            { "cucumber", Theme::Cucumber() },
            { "plain", Theme::Plain() },
            { "exclude-attachments", Theme::Plain() },
        };

        ASSERT_THAT(themes, testing::Contains(testing::Key(GetParam().theme)));

        Validate(GetParam().input, GetParam().output,
            SummaryPrinter::Factory{}
                .Theme(themes.at(GetParam().theme))
                .Options(SummaryPrinter::Options::includeAttachments, GetParam().theme != "exclude-attachments")
                .Build(stream));
    }

    TEST_P(ProgressFormatterTest, TestData)
    {
        std::map<std::string_view, std::shared_ptr<Theme>> themes{
            { "cucumber", Theme::Cucumber() },
            { "plain", Theme::Plain() },
        };

        ASSERT_THAT(themes, testing::Contains(testing::Key(GetParam().theme)));

        Validate(GetParam().input, GetParam().output, std::make_unique<ProgressPrinter>(stream, themes.at(GetParam().theme), 80));
    }

    TEST_P(ProgressBarFormatterTest, TestData)
    {
        FakeTty fakeTty;

        std::map<std::string_view, std::unique_ptr<Theme>> themes;
        themes.try_emplace("cucumber", Theme::Cucumber());

        ASSERT_THAT(themes, testing::Contains(testing::Key(GetParam().theme)));

        Validate(GetParam().input, GetParam().output,
            std::make_unique<ProgressBarPrinter>(
                fakeTty, std::move(themes.at(GetParam().theme)), 50,
                [](std::string uri) -> std::string
                {
                    return uri;
                },
                std::set<ProgressBarPrinter::Options>{ ProgressBarPrinter::Options ::includeAttachments }),
            fakeTty);
    }

    INSTANTIATE_TEST_SUITE_P(Acceptance, PrettyFormatterTest, testing::ValuesIn(EnumerateTestData("pretty")));
    INSTANTIATE_TEST_SUITE_P(Acceptance, SummaryFormatterTest, testing::ValuesIn(EnumerateTestData("summary")));
    INSTANTIATE_TEST_SUITE_P(Acceptance, ProgressFormatterTest, testing::ValuesIn(EnumerateTestData("progress")));
    INSTANTIATE_TEST_SUITE_P(Acceptance, ProgressBarFormatterTest, testing::ValuesIn(EnumerateTestData("progressbar")));
}
