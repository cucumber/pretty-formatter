#include "cucumber/messages/TestStepResultStatus.hpp"
#include "cucumber/pretty-formatter/Theme.hpp"
#include "gtest/gtest.h"
#include <string>

namespace cucumber::pretty_formatter
{

    namespace
    {
        struct TestTheme : public testing::Test
        {};
    }

    TEST_F(TestTheme, themetest)
    {
        const auto none = Theme::Factory{}.Build();

        const auto plain = Theme::Factory{}
                               .ProgressIcon(messages::TestStepResultStatus::AMBIGUOUS, "A")
                               .StatusIcon(messages::TestStepResultStatus::AMBIGUOUS, "B")
                               .ProgressIcon(messages::TestStepResultStatus::AMBIGUOUS, "A")
                               .Build();

        const auto demo = Theme::Factory{}
                              .ProgressIcon(messages::TestStepResultStatus::AMBIGUOUS, "A")
                              .StatusIcon(messages::TestStepResultStatus::AMBIGUOUS, "B")
                              .ProgressIcon(messages::TestStepResultStatus::AMBIGUOUS, "A")
                              .Build();

        const auto cucumber = Theme::Factory{}
                                  .ProgressIcon(messages::TestStepResultStatus::AMBIGUOUS, "A")
                                  .StatusIcon(messages::TestStepResultStatus::AMBIGUOUS, "B")
                                  .ProgressIcon(messages::TestStepResultStatus::AMBIGUOUS, "A")
                                  .StatusIcon(messages::TestStepResultStatus::AMBIGUOUS, "B")
                                  .ProgressIcon(messages::TestStepResultStatus::AMBIGUOUS, "A")
                                  .StatusIcon(messages::TestStepResultStatus::AMBIGUOUS, "B")
                                  .ProgressIcon(messages::TestStepResultStatus::AMBIGUOUS, "A")
                                  .StatusIcon(messages::TestStepResultStatus::AMBIGUOUS, "B")
                                  .ProgressIcon(messages::TestStepResultStatus::AMBIGUOUS, "A")
                                  .StatusIcon(messages::TestStepResultStatus::AMBIGUOUS, "B")
                                  .ProgressIcon(messages::TestStepResultStatus::AMBIGUOUS, "A")
                                  .StatusIcon(messages::TestStepResultStatus::AMBIGUOUS, "B")
                                  .Build();
    }
}
