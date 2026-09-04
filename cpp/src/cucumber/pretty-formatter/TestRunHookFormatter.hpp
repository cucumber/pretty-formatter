#ifndef CUCUMBER_PRETTY_FORMATTER_TEST_RUN_HOOK_FORMATTER_HPP
#define CUCUMBER_PRETTY_FORMATTER_TEST_RUN_HOOK_FORMATTER_HPP

#include "cucumber/messages/TestRunHookFinished.hpp"
#include "cucumber/pretty-formatter/LineBuilder.hpp"
#include "cucumber/pretty-formatter/SourceReferenceFormatter.hpp"
#include "cucumber/pretty-formatter/Theme.hpp"
#include "cucumber/query/Query.hpp"
#include <cstddef>
#include <memory>
#include <string>

namespace cucumber::pretty_formatter
{
    struct TestRunHookFormatter
    {
        TestRunHookFormatter(query::Query& data, std::shared_ptr<Theme> theme, SourceReferenceFormatter sourceReferenceFormatter,
            std::size_t indent);

        ~TestRunHookFormatter() = default;

        TestRunHookFormatter(const TestRunHookFormatter&) = delete;
        TestRunHookFormatter(TestRunHookFormatter&&) = delete;

        TestRunHookFormatter& operator=(const TestRunHookFormatter&) = delete;
        TestRunHookFormatter& operator=(TestRunHookFormatter&&) = delete;

        void FormatHookLineTo(LineBuilder& lineBuilder,
            const std::shared_ptr<const messages::TestRunHookFinished>& testRunHookFinished) const;

        [[nodiscard]] std::string FormatException(const std::shared_ptr<const messages::TestRunHookFinished>& testRunHookFinished) const;

    private:
        query::Query& data;
        std::shared_ptr<Theme> theme;
        SourceReferenceFormatter sourceReferenceFormatter;
        std::size_t indent;
    };
}

#endif
