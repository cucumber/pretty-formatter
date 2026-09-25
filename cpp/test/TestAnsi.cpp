#include "cucumber/messages/TestStepResultStatus.hpp"
#include "cucumber/pretty-formatter/Ansi.hpp"
#include "cucumber/pretty-formatter/Theme.hpp"
#include "gmock/gmock.h"
#include "gtest/gtest.h"
#include <fstream>
#include <sstream>
#include <string>

namespace cucumber::pretty_formatter
{

    namespace
    {
        struct TestAnsi : public testing::Test
        {};
    }

    TEST_F(TestAnsi, ansitest)
    {
        Ansi bold{ Ansi::Attribute::bold };
        Ansi boldOff{ Ansi::Attribute::boldOff };

        std::ostringstream out;
        out << "\n" << bold.ToString() << "Feature:" << boldOff.ToString() << " All statuses";

        const auto* expected = "\n\x1B[1mFeature:\x1B[22m All statuses";
        ASSERT_THAT(out.str(), testing::Eq(expected));
    }
}
