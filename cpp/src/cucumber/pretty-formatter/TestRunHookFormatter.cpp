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

    void TestRunHookFormatter::FormatHookLineTo(LineBuilder& lineBuilder,
        const std::shared_ptr<const messages::TestRunHookFinished>& testRunHookFinished) const
    {
        const auto& optHook = data.FindHookBy(testRunHookFinished);
        if (optHook.has_value())
        {
            const auto& hook = optHook.value();
            lineBuilder.Append(HookTypeName(hook->type))
                .Accept(
                    [&hook](LineBuilder& lineBuilder)
                    {
                        if (hook->name.has_value())
                        {
                            lineBuilder.Append(fmt::format("({})", hook->name.value()));
                        }
                    })
                .Accept(
                    [this, &hook](LineBuilder& lineBuilder)
                    {
                        AppendLocationComment(lineBuilder, sourceReferenceFormatter, hook->sourceReference);
                    });
        }
    }

    std::string TestRunHookFormatter::FormatException(const std::shared_ptr<const messages::TestRunHookFinished>& testRunHookFinished) const
    {
        return FormatResultException(testRunHookFinished->result, indent, theme);
    }
}
