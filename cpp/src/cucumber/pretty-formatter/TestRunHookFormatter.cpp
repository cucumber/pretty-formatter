#include "cucumber/pretty-formatter/TestRunHookFormatter.hpp"
#include "cucumber/messages/TestRunHookFinished.hpp"
#include "cucumber/pretty-formatter/FormatResultException.hpp"
#include "cucumber/pretty-formatter/HookTypeName.hpp"
#include "cucumber/pretty-formatter/LineBuilder.hpp"
#include "cucumber/pretty-formatter/LocationComment.hpp"
#include "cucumber/pretty-formatter/SourceReferenceFormatter.hpp"
#include "cucumber/pretty-formatter/Theme.hpp"
#include "cucumber/query/Query.hpp"
#include <cstddef>
#include <fmt/core.h>
#include <fmt/format.h>
#include <memory>
#include <string>
#include <utility>

namespace cucumber::pretty_formatter
{
    TestRunHookFormatter::TestRunHookFormatter(query::Query& data, std::shared_ptr<Theme> theme,
        SourceReferenceFormatter sourceReferenceFormatter, std::size_t indent)
        : data{ data }
        , theme{ std::move(theme) }
        , sourceReferenceFormatter{ std::move(sourceReferenceFormatter) }
        , indent{ indent }
    {}

    void TestRunHookFormatter::FormatHookLineTo(LineBuilder& lineBuilder, const messages::TestRunHookFinished& testRunHookFinished) const
    {
        const auto* hook = data.FindHookBy(testRunHookFinished);
        if (hook != nullptr)
        {
            lineBuilder.Append(HookTypeName(hook->type))
                .Accept(
                    [&hook](LineBuilder& lineBuilder)
                    {
                        if (hook->name)
                        {
                            lineBuilder.Append(fmt::format("({})", *hook->name));
                        }
                    })
                .Accept(
                    [this, &hook](LineBuilder& lineBuilder)
                    {
                        AppendLocationComment(lineBuilder, sourceReferenceFormatter, hook->sourceReference);
                    });
        }
    }

    std::string TestRunHookFormatter::FormatException(const messages::TestRunHookFinished& testRunHookFinished) const
    {
        return FormatResultException(testRunHookFinished.result, indent, theme);
    }
}
