#include "cucumber/pretty-formatter/FormatSnippets.hpp"
#include "cucumber/messages/Snippet.hpp"
#include "cucumber/pretty-formatter/AsUnary.hpp"
#include "cucumber/pretty-formatter/PickleComparator.hpp"
#include "cucumber/query/Query.hpp"
#include <algorithm>
#include <fmt/core.h>
#include <fmt/format.h>
#include <iterator>
#include <memory>
#include <optional>
#include <string>
#include <unordered_set>
#include <vector>

namespace cucumber::pretty_formatter
{
    std::optional<std::string> FormatSnippets(const query::Query& query)
    {
        const auto& allTestCasesFinishedOrdered =
            query.FindAllTestCaseFinishedOrderBy(query::findPickleByTestCaseFinished, PickleComparator);

        std::vector<std::shared_ptr<const messages::Snippet>> snippets;
        std::unordered_set<std::string> seen;

        for (const auto& testCaseFinished : allTestCasesFinishedOrdered)
        {
            const auto& optPickle = query.FindPickleBy(testCaseFinished);
            if (optPickle.has_value())
            {
                const auto& pickle = optPickle.value();
                const auto& suggestions = query.FindSuggestionsBy(pickle);

                for (const auto& suggestion : suggestions)
                {
                    for (const auto& snippet : suggestion->snippets)
                    {
                        if (seen.insert(snippet->language + "-" + snippet->code).second)
                        {
                            snippets.push_back(snippet);
                        }
                    }
                }
            }
        }

        if (snippets.empty())
        {
            return std::nullopt;
        }

        std::vector<std::string> lines{ "", "You can implement missing steps with the snippets below:", "" };
        for (const auto& snippet : snippets)
        {
            lines.emplace_back(snippet->code);
            lines.emplace_back("");
        }
        return fmt::format("{}", fmt::join(lines, "\n"));
    }
}
