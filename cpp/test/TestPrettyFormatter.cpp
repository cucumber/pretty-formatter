#include "cucumber/pretty-formatter/Formatter.hpp"
#include "cucumber/pretty-formatter/PrettyPrinter.hpp"
#include "cucumber/pretty-formatter/ProgressBarPrinter.hpp"
#include "cucumber/pretty-formatter/ProgressPrinter.hpp"
#include "cucumber/pretty-formatter/SummaryPrinter.hpp"
#include "cucumber/pretty-formatter/Theme.hpp"
#include "gmock/gmock.h"
#include "gtest/gtest.h"
#include <cctype>
#include <filesystem>
#include <map>
#include <memory>
#include <ostream>
#include <string>
#include <string_view>
#include <vector>

namespace cucumber::pretty_formatter
{
    namespace
    {
        std::string Sanitize(std::string_view text)
        {
            std::string result;
            for (const char c : text)
            {
                result += std::isalnum(static_cast<unsigned char>(c)) ? c : '_';
            }
            return result.empty() ? std::string{ "empty" } : result;
        }

        struct FormatterTestParam
        {
            std::filesystem::path input;
            std::filesystem::path output;
            std::string theme;
            std::string formatter;
        };

        void PrintTo(const FormatterTestParam& param, std::ostream* stream)
        {
            *stream << param.input.stem().string() << "." << param.formatter << "." << param.theme;
        }

        std::vector<FormatterTestParam> EnumerateTestData()
        {
            std::vector<FormatterTestParam> result;
            const std::filesystem::path testdataPath{ std::filesystem::path{ TESTDATA_SRC } };

            for (const auto& entry : std::filesystem::directory_iterator(testdataPath))
            {
                if (entry.is_regular_file() && entry.path().extension() == ".log")
                {
                    auto& test = result.emplace_back();

                    test.output = entry.path();

                    auto filename = entry.path().stem().string();

                    test.input = filename.substr(0, filename.find_first_of('.'));
                    filename = filename.substr(filename.find_first_of('.') + 1);

                    test.theme = filename.substr(0, filename.find_first_of('.'));
                    filename = filename.substr(filename.find_first_of('.') + 1);

                    test.formatter = filename.substr(filename.find_first_of('.') + 1);
                }
            }

            return result;
        }

        struct PrettyFormatterTest : public testing::TestWithParam<FormatterTestParam>
        {};
    }

    TEST_P(PrettyFormatterTest, FormatterTest)
    {
        std::map<std::string_view, std::shared_ptr<Formatter>> formatters{
            { "pretty", std::make_shared<PrettyPrinter>() },
            { "progress", std::make_shared<ProgressPrinter>() },
            { "progressbar", std::make_shared<ProgressBarPrinter>() },
            { "summary", std::make_shared<SummaryPrinter>() },
        };

        std::map<std::string_view, Theme> themes{
            { "cucumber", Theme{} },
            { "demo", Theme{} },
            { "plain", Theme{} },
            { "none", Theme{} },
            { "exclude-features-and-rules", Theme{} },
            { "exclude-attachments", Theme{} },
        };

        ASSERT_THAT(formatters, testing::Contains(testing::Key(GetParam().formatter)));
        ASSERT_THAT(themes, testing::Contains(testing::Key(GetParam().theme)));
    }

    INSTANTIATE_TEST_SUITE_P(Acceptance, PrettyFormatterTest, testing::ValuesIn(EnumerateTestData()));
}
