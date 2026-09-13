#ifndef CPP_TEST_ENUMERATE_TEST_DATA_HPP
#define CPP_TEST_ENUMERATE_TEST_DATA_HPP

#include <filesystem>
#include <ostream>
#include <string>
#include <string_view>
#include <vector>

namespace cucumber::pretty_formatter
{
    struct FormatterTestParam
    {
        std::filesystem::path input;
        std::filesystem::path output;
        std::string theme;
        std::string formatter;
    };

    void PrintTo(const FormatterTestParam& param, std::ostream* stream);

    std::vector<FormatterTestParam> EnumerateTestData(std::string_view formatter);
}

#endif
