#include "cucumber/pretty-formatter/InterferenceInterceptor.hpp"
#include "gmock/gmock.h"
#include "gtest/gtest.h"
#include <ostream>
#include <sstream>
#include <string>
#include <string_view>

namespace cucumber::pretty_formatter
{
    namespace
    {
        struct TestInterferenceInterceptor : testing::Test
        {
            std::ostringstream output;
            std::string reported;

            InterferenceInterceptor interceptor{ [this](std::string_view content)
                {
                    reported = content;
                },
                { output } };
        };

        struct TestInterferenceInterceptorAppend : testing::Test
        {
            std::ostringstream output;
            std::string reported;

            InterferenceInterceptor interceptor{ [this](std::string_view content)
                {
                    reported += content;
                },
                { output } };
        };
    }

    TEST_F(TestInterferenceInterceptor, ReportsCapturedOutputWhenReleased)
    {
        interceptor.Acquire();
        output << "interference";
        interceptor.Release();

        ASSERT_THAT(output.str(), testing::IsEmpty());
        ASSERT_THAT(reported, testing::Eq("interference"));
    }

    TEST_F(TestInterferenceInterceptor, BypassWritesWithoutCapturing)
    {
        interceptor.Acquire();
        output << "captured ";
        interceptor.Bypass(
            [this]
            {
                output << "bypassed ";
            });
        output << "again";
        interceptor.Release();

        ASSERT_THAT(output.str(), testing::Eq("bypassed "));
        ASSERT_THAT(reported, testing::Eq("captured again"));
    }

    TEST_F(TestInterferenceInterceptor, RestoresStreamsWhenDestroyed)
    {
        interceptor.Acquire();
        output << "captured";
        interceptor.Release();

        output << " restored";

        ASSERT_THAT(reported, testing::Eq("captured"));
        ASSERT_THAT(output.str(), testing::Eq(" restored"));
    }

    TEST_F(TestInterferenceInterceptorAppend, CanBeAcquiredAndReleasedMoreThanOnce)
    {
        interceptor.Acquire();
        output << "one";
        interceptor.Release();
        interceptor.Acquire();
        output << "two";
        interceptor.Release();

        ASSERT_THAT(reported, testing::Eq("onetwo"));
    }
}
