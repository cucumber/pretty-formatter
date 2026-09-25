#include "EnumerateTestData.hpp"
#include <filesystem>
#include <optional>
#include <ostream>
#include <regex>
#include <string_view>
#include <utility>
#include <vector>

namespace cucumber::pretty_formatter
{
    namespace
    {
        const std::regex testDataFilenameRegex{ R"((.+)\.(.+)\.(.+))" };

        std::optional<FormatterTestParam> ParseTestDataFilename(const std::filesystem::path& path)
        {
            std::smatch match;
            const auto filename = path.stem().string();

            if (std::regex_match(filename, match, testDataFilenameRegex))
            {
                FormatterTestParam result;
                result.input = path.parent_path() / (match[1].str() + ".ndjson");
                result.output = path;
                result.theme = match[2].str();
                result.formatter = match[3].str();
                return result;
            }

            return std::nullopt;
        }
    }

    void PrintTo(const FormatterTestParam& param, std::ostream* stream)
    {
        *stream << param.input.stem().string() << "." << param.theme << "." << param.formatter;
    }

    std::vector<FormatterTestParam> EnumerateTestData(std::string_view formatter)
    {
        std::vector<FormatterTestParam> result;

        const std::filesystem::path testdataPath{ std::filesystem::path{ TESTDATA_SRC } };

        for (const auto& entry : std::filesystem::directory_iterator(testdataPath))
        {
            if (entry.is_regular_file() && entry.path().extension() == ".log")
            {
                if (auto testData = ParseTestDataFilename(entry.path()); testData.has_value() && testData->formatter == formatter)
                {
                    result.emplace_back(std::move(*testData));
                }
            }
        }

        return result;
    }
}
