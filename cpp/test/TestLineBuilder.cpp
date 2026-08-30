#include "cucumber/pretty-formatter/LineBuilder.hpp"
#include "cucumber/pretty-formatter/Theme.hpp"
#include "gmock/gmock.h"
#include "gtest/gtest.h"
#include <cstddef>
#include <memory>
#include <string>

namespace cucumber::pretty_formatter
{
    namespace
    {
        struct TestLineBuilder : public testing::Test
        {
            LineBuilder builder{ std::make_shared<Theme>() };
        };
    }

    TEST_F(TestLineBuilder, should_build_line)
    {
        builder.Append("Hello").Append(" ").Append("World");
        EXPECT_THAT(builder.Build(), testing::StrEq("Hello World"));
    }

    TEST_F(TestLineBuilder, build_multiple_lines)
    {
        builder.Append("Hello").NewLine().Append("World");
        EXPECT_THAT(builder.Build(), testing::StrEq("Hello\nWorld"));
    }

    TEST_F(TestLineBuilder, build_indented_line)
    {
        static constexpr std::size_t indent = 10;
        builder.Append("Hello").Indent(indent).Append("World");
        EXPECT_THAT(builder.Build(), testing::StrEq("Hello     World"));
    }

    TEST_F(TestLineBuilder, build_indented_line_indent_less_than_current_length)
    {
        builder.Append("Hello").Indent(1).Append("World");
        EXPECT_THAT(builder.Build(), testing::StrEq("HelloWorld"));
    }
}
