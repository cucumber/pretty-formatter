package io.cucumber.prettyformatter;

import io.cucumber.messages.types.PickleTable;
import io.cucumber.messages.types.PickleTableCell;

import java.util.List;
import java.util.function.Function;

import static java.lang.System.lineSeparator;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

final class PickleTableFormatter {

    private final Function<Integer, String> rowPrefix;

    private PickleTableFormatter(Function<Integer, String> rowPrefix) {
        this.rowPrefix = rowPrefix;
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

    private static void renderCellWithPadding(String cellText, int padding, StringBuilder result) {
        result.append(cellText);
        padSpace(result, padding);
    }

    private static void padSpace(StringBuilder result, int padding) {
        for (int i = 0; i < padding; i++) {
            result.append(" ");
        }
    }

    String format(PickleTable pickleTable) {
        List<List<String>> cells = pickleTable.getRows().stream()
                .map(pickleTableRow -> pickleTableRow.getCells().stream().map(PickleTableCell::getValue)
                        .collect(toList()))
                .collect(toList());
        return format(cells);
    }

    private String format(List<List<String>> table) {
        requireNonNull(table, "table may not be null");

        if (table.isEmpty()) {
            return "";
        }
        // render the individual cells
        String[][] renderedCells = renderCells(table);
        // find the length of longest cell per column
        int[] longestCellLengthInColumn = findLongestCellLengthInColumn(renderedCells);
        // print the rendered cells with padding
        return renderTableWithPadding(renderedCells, longestCellLengthInColumn);
    }

    private String renderTableWithPadding(String[][] renderedCells, int[] longestCellLengthInColumn) {
        StringBuilder result = new StringBuilder();
        // datatables are always square and non-sparse.
        int height = renderedCells.length;
        int width = renderedCells[0].length;
        for (int rowIndex = 0; rowIndex < height; rowIndex++) {
            printRowPrefix(result, rowIndex);
            result.append("| ");
            for (int colIndex = 0; colIndex < width; colIndex++) {
                String cellText = renderedCells[rowIndex][colIndex];
                int padding = longestCellLengthInColumn[colIndex] - cellText.length();
                renderCellWithPadding(cellText, padding, result);
                if (colIndex < width - 1) {
                    result.append(" | ");
                } else {
                    result.append(" |");
                }
            }
            result.append(lineSeparator());
        }
        return result.toString();
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

    private void printRowPrefix(StringBuilder buffer, int rowIndex) {
        String prefix = rowPrefix.apply(rowIndex);
        if (prefix != null) {
            buffer.append(prefix);
        }
    }

    private String renderCell(String cell) {
        if (cell == null) {
            return "";
        }
        return cell;
    }

    static final class Builder {
        private Function<Integer, String> rowPrefix = rowIndex -> "";

        Builder prefixRow(Function<Integer, String> rowPrefix) {
            requireNonNull(rowPrefix, "rowPrefix may not be null");
            this.rowPrefix = rowPrefix;
            return this;
        }

        Builder prefixRow(String rowPrefix) {
            requireNonNull(rowPrefix, "rowPrefix may not be null");
            return prefixRow(rowIndex -> rowPrefix);
        }

        PickleTableFormatter build() {
            return new PickleTableFormatter(rowPrefix);
        }
    }

}
