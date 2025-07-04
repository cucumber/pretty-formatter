package io.cucumber.prettyformatter;

import io.cucumber.messages.types.PickleTable;
import io.cucumber.messages.types.PickleTableCell;

import java.util.List;

import static io.cucumber.prettyformatter.Theme.Element.DATA_TABLE;
import static io.cucumber.prettyformatter.Theme.Element.DATA_TABLE_BORDER;
import static io.cucumber.prettyformatter.Theme.Element.DATA_TABLE_CONTENT;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

final class PickleTableFormatter {

    private final int indentation;

    private PickleTableFormatter(int indentation) {
        this.indentation = indentation;
    }

    static Builder builder() {
        return new Builder();
    }

    private static int[] findLongestCellLengthInColumn(String[][] renderedCells) {
        // datatables are always square and non-sparse.
        int width = renderedCells[0].length;
        int[] longestCellInColumnLength = new int[width];
        for (String[] row : renderedCells) {
            for (int colIndex = 0; colIndex < width; colIndex++) {
                int current = longestCellInColumnLength[colIndex];
                int candidate = row[colIndex].length();
                longestCellInColumnLength[colIndex] = Math.max(current, candidate);
            }
        }
        return longestCellInColumnLength;
    }

    private static String renderCellWithPadding(String cellText, int padding) {
        StringBuilder result = new StringBuilder();
        result.append(" ");
        result.append(cellText);
        padSpace(result, padding);
        result.append(" ");
        return result.toString();
    }

    private static void padSpace(StringBuilder result, int padding) {
        for (int i = 0; i < padding; i++) {
            result.append(" ");
        }
    }

    private static void renderTableRowWithPaddingTo(String[] renderedCell, int[] longestCellLengthInColumn, LineBuilder lineBuilder) {
        int width = renderedCell.length;
        for (int colIndex = 0; colIndex < width; colIndex++) {
            String cellText = renderedCell[colIndex];
            int padding = longestCellLengthInColumn[colIndex] - cellText.length();
            lineBuilder
                    .append(DATA_TABLE_CONTENT, renderCellWithPadding(cellText, padding))
                    .append(DATA_TABLE_BORDER, "|");
        }
    }

    void formatTo(PickleTable pickleTable, LineBuilder lineBuilder) {
        List<List<String>> cells = pickleTable.getRows().stream()
                .map(pickleTableRow -> pickleTableRow.getCells().stream().map(PickleTableCell::getValue)
                        .collect(toList()))
                .collect(toList());
        formatTo(cells, lineBuilder);
    }

    private void formatTo(List<List<String>> table, LineBuilder lineBuilder) {
        requireNonNull(table, "table may not be null");

        if (table.isEmpty()) {
            return;
        }
        // render the individual cells
        String[][] renderedCells = renderCells(table);
        // find the length of longest cell per column
        int[] longestCellLengthInColumn = findLongestCellLengthInColumn(renderedCells);
        // print the rendered cells with padding
        renderTableWithPaddingTo(renderedCells, longestCellLengthInColumn, lineBuilder);
    }

    private void renderTableWithPaddingTo(String[][] renderedCells, int[] longestCellLengthInColumn, LineBuilder lineBuilder) {
        for (String[] renderedCell : renderedCells) {
            lineBuilder.indent(indentation)
                    .begin(DATA_TABLE)
                    .append(DATA_TABLE_BORDER, "|")
                    .accept(innerLineBuilder -> renderTableRowWithPaddingTo(renderedCell, longestCellLengthInColumn, innerLineBuilder))
                    .end(DATA_TABLE)
                    .newLine();
        }
    }

    private String[][] renderCells(List<List<String>> table) {
        // datatables are always square and non-sparse.
        int height = table.size();
        int width = table.get(0).size();
        String[][] renderedCells = new String[height][width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                renderedCells[i][j] = renderCell(table.get(i).get(j));
            }
        }
        return renderedCells;
    }

    private String renderCell(String cell) {
        if (cell == null) {
            return "";
        }
        return cell;
    }

    static final class Builder {
        private int indentation = 0;

        Builder indentation(int indentation) {
            this.indentation = indentation;
            return this;
        }

        PickleTableFormatter build() {
            return new PickleTableFormatter(indentation);
        }
    }

}
