#ifndef CUCUMBER_PRETTY_FORMATTER_PICKLE_TABLE_FORMATTER_HPP
#define CUCUMBER_PRETTY_FORMATTER_PICKLE_TABLE_FORMATTER_HPP

#include "cucumber/messages/PickleTable.hpp"
#include "cucumber/pretty-formatter/LineBuilder.hpp"
#include <cstddef>
#include <memory>
#include <string>
#include <vector>

namespace cucumber::pretty_formatter
{
    struct PickleTableFormatter
    {
        explicit PickleTableFormatter(std::size_t indent);

        void Format(LineBuilder& lineBuilder, const std::shared_ptr<const messages::PickleTable>& pickleTable);

    private:
        void Format(LineBuilder& lineBuilder, const std::vector<std::vector<std::string>>& cells) const;

        [[nodiscard]] std::vector<std::size_t> CalculateColumnWidths(const std::vector<std::vector<std::string>>& cells) const;

        void RenderTableWithPadding(const std::vector<std::vector<std::string>>& cells, const std::vector<std::size_t>& columnWidths,
            LineBuilder& lineBuilder) const;

        void RenderTableRowWithPadding(const std::vector<std::string>& cells, const std::vector<std::size_t>& columnWidths,
            LineBuilder& lineBuilder) const;

        std::size_t indent;
    };
}

#endif
