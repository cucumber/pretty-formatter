
#include "cucumber/pretty-formatter/PickleStepArgumentFormatter.hpp"
#include "cucumber/messages/PickleDocString.hpp"
#include "cucumber/messages/PickleStepArgument.hpp"
#include "cucumber/messages/PickleTable.hpp"
#include "cucumber/pretty-formatter/LineBuilder.hpp"
#include "cucumber/pretty-formatter/PickleDocStringFormatter.hpp"
#include "cucumber/pretty-formatter/PickleTableFormatter.hpp"
#include "cucumber/pretty-formatter/Theme.hpp"
#include "fmt/core.h"
#include "fmt/format.h"
#include "fmt/ostream.h"
#include <cstddef>
#include <memory>
#include <ostream>
#include <utility>

namespace cucumber::pretty_formatter
{
    namespace
    {
        void PrintDataTable(std::ostream& stream, std::shared_ptr<Theme> theme, std::size_t indent,
            const messages::PickleStepArgument& pickleStepArgument)
        {
            if (pickleStepArgument.dataTable)
            {
                fmt::print(stream, "{}",
                    LineBuilder{ std::move(theme) }
                        .Accept(
                            [&indent, &pickleStepArgument](LineBuilder& lineBuilder)
                            {
                                PickleTableFormatter{ indent }.Format(lineBuilder, *pickleStepArgument.dataTable);
                            })
                        .Build());
            }
        }

        void PrintDocString(std::ostream& stream, std::shared_ptr<Theme> theme, std::size_t indent,
            const messages::PickleStepArgument& pickleStepArgument)
        {
            if (pickleStepArgument.docString)
            {
                fmt::print(stream, "{}",
                    LineBuilder{ std::move(theme) }
                        .Accept(
                            [&indent, &pickleStepArgument](LineBuilder& lineBuilder)
                            {
                                PickleDocStringFormatter{ indent }.Format(lineBuilder, *pickleStepArgument.docString);
                            })
                        .Build());
            }
        }
    }

    PickleStepArgumentFormatter::PickleStepArgumentFormatter(std::ostream& stream, std::shared_ptr<Theme> theme, std::size_t indent)
        : stream{ stream }
        , theme{ std::move(theme) }
        , indent{ indent }
    {}

    void PickleStepArgumentFormatter::Format(const messages::PickleStepArgument& pickleStepArgument)
    {
        auto pickleDataTableIndex = pickleStepArgument.dataTable.value_or(messages::PickleTable{}).argumentIndex.value_or(-1);
        auto pickleDocStringIndex = pickleStepArgument.docString.value_or(messages::PickleDocString{}).argumentIndex.value_or(-1);

        if (pickleDataTableIndex < pickleDocStringIndex)
        {
            PrintDataTable(stream, theme, indent, pickleStepArgument);
            PrintDocString(stream, theme, indent, pickleStepArgument);
        }
        else
        {
            PrintDocString(stream, theme, indent, pickleStepArgument);
            PrintDataTable(stream, theme, indent, pickleStepArgument);
        }
    }
}
