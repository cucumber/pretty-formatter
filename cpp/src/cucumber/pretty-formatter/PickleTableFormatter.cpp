#include "cucumber/pretty-formatter/PickleTableFormatter.hpp"
#include "cucumber/messages/PickleTable.hpp"
#include "cucumber/pretty-formatter/LineBuilder.hpp"
#include "cucumber/pretty-formatter/Theme.hpp"
#include <algorithm>
#include <cstddef>
#include <fmt/format.h>
#include <memory>
#include <string>
#include <vector>

namespace cucumber::pretty_formatter
{
    PickleTableFormatter::PickleTableFormatter(std::size_t indent)
        : indent{ indent }
    {}

    void PickleTableFormatter::Format(LineBuilder& lineBuilder, const std::shared_ptr<const messages::PickleTable>& pickleTable)
    {
        std::vector<std::vector<std::string>> cells;
        for (const auto& row : pickleTable->rows)
        {
            auto& cellValues = cells.emplace_back();

            for (const auto& cell : row->cells)
            {
                cellValues.push_back(cell->value);
            }
        }

        Format(lineBuilder, cells);
    }

    void PickleTableFormatter::Format(LineBuilder& lineBuilder, const std::vector<std::vector<std::string>>& cells) const
    {
        if (cells.empty())
        {
            return;
        }

        const auto columnWidths = CalculateColumnWidths(cells);
        RenderTableWithPadding(cells, columnWidths, lineBuilder);
    }

    std::vector<std::size_t> PickleTableFormatter::CalculateColumnWidths(const std::vector<std::vector<std::string>>& cells) const
    {
        std::vector<std::size_t> columnWidths;

        for (const auto& row : cells)
        {
            for (std::size_t i = 0; i < row.size(); ++i)
            {
                if (columnWidths.size() <= i)
                {
                    columnWidths.push_back(row[i].size());
                }
                else
                {
                    columnWidths[i] = std::max(columnWidths[i], row[i].size());
                }
            }
        }

        return columnWidths;
    }

    void PickleTableFormatter::RenderTableWithPadding(const std::vector<std::vector<std::string>>& cells,
        const std::vector<std::size_t>& columnWidths, LineBuilder& lineBuilder) const
    {
        for (const auto& row : cells)
        {
            lineBuilder.Indent(indent)
                .Begin(Theme::Element::dataTable)
                .Append(Theme::Element::dataTableBorder, "|")
                .Accept(
                    [this, &row, &columnWidths](auto& lineBuilder)
                    {
                        RenderTableRowWithPadding(row, columnWidths, lineBuilder);
                    })
                .End(Theme::Element::dataTable)
                .NewLine();
        }
    }

    void PickleTableFormatter::RenderTableRowWithPadding(const std::vector<std::string>& cells,
        const std::vector<std::size_t>& columnWidths, LineBuilder& lineBuilder) const
    {
        for (std::size_t colIndex{ 0 }; colIndex < cells.size(); ++colIndex)
        {
            const auto padding = columnWidths[colIndex] - cells[colIndex].size();
            lineBuilder.Append(Theme::Element::dataTableContent, fmt::format(" {}{} ", cells[colIndex], std::string(padding, ' ')))
                .Append(Theme::Element::dataTableBorder, "|");
        }
    }
}
