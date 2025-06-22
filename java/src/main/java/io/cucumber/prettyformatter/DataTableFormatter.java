package io.cucumber.prettyformatter;

import io.cucumber.messages.types.PickleTable;
import io.cucumber.messages.types.PickleTableCell;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

// TODO: Move?
final class DataTableFormatter {

    private final Function<Integer, String> rowPrefix;
    private final boolean escapeDelimiters;

    private DataTableFormatter(Function<Integer, String> rowPrefix, boolean escapeDelimiters) {
        this.rowPrefix = rowPrefix;
        this.escapeDelimiters = escapeDelimiters;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String format(List<List<String>> table) {
        StringBuilder result = new StringBuilder();
        formatTo(table, result);
        return result.toString();
    }

    public void formatTo(List<List<String>> table, StringBuilder appendable) {
        try {
            formatTo(table, (Appendable) appendable);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public void formatTo(List<List<String>> table, Appendable appendable) throws IOException {
        requireNonNull(table, "table may not be null");
        requireNonNull(appendable, "appendable may not be null");

        if (table.isEmpty()) {
            return;
        }
        // datatables are always square and non-sparse.
        int height = table.size();
        int width = table.get(0).size();

        // render the individual cells
        String[][] renderedCells = new String[height][width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                renderedCells[i][j] = renderCell(table.get(i).get(j));
            }
        }

        // find the longest rendered cell in each column
        int[] longestCellInColumnLength = new int[width];
        for (String[] row : renderedCells) {
            for (int colIndex = 0; colIndex < width; colIndex++) {
                int current = longestCellInColumnLength[colIndex];
                int candidate = row[colIndex].length();
                longestCellInColumnLength[colIndex] = Math.max(current, candidate);
            }
        }

        // print the rendered cells with padding
        for (int rowIndex = 0; rowIndex < height; rowIndex++) {
            printRowPrefix(appendable, rowIndex);
            appendable.append("| ");
            for (int colIndex = 0; colIndex < width; colIndex++) {
                String cellText = renderedCells[rowIndex][colIndex];
                appendable.append(cellText);
                int padding = longestCellInColumnLength[colIndex] - cellText.length();
                padSpace(appendable, padding);
                if (colIndex < width - 1) {
                    appendable.append(" | ");
                } else {
                    appendable.append(" |");
                }
            }
            appendable.append(System.lineSeparator());
        }
    }

    void printRowPrefix(Appendable buffer, int rowIndex) throws IOException {
        String prefix = rowPrefix.apply(rowIndex);
        if (prefix != null) {
            buffer.append(prefix);
        }
    }

    private String renderCell(String cell) {
        if (cell == null) {
            return "";
        }

        if (cell.isEmpty()) {
            return "[empty]";
        }

        if (!escapeDelimiters) {
            return cell;
        }

        return cell
                .replaceAll("\\\\(?!\\|)", "\\\\\\\\")
                .replaceAll("\\n", "\\\\n")
                .replaceAll("\\|", "\\\\|");
    }

    private void padSpace(Appendable buffer, int indent) throws IOException {
        for (int i = 0; i < indent; i++) {
            buffer.append(" ");
        }
    }

    public String format(PickleTable pickleTable) {
        List<List<String>> cells = pickleTable.getRows().stream()
                .map(pickleTableRow -> pickleTableRow.getCells().stream().map(PickleTableCell::getValue)
                        .collect(toList()))
                .collect(toList());
        return format(cells);
    }

    static final class Builder {
        private Function<Integer, String> rowPrefix = rowIndex -> "";
        private boolean escapeDelimiters = true;

        public Builder prefixRow(Function<Integer, String> rowPrefix) {
            requireNonNull(rowPrefix, "rowPrefix may not be null");
            this.rowPrefix = rowPrefix;
            return this;
        }

        public Builder prefixRow(String rowPrefix) {
            requireNonNull(rowPrefix, "rowPrefix may not be null");
            return prefixRow(rowIndex -> rowPrefix);
        }

        public Builder escapeDelimiters(boolean escapeDelimiters) {
            this.escapeDelimiters = escapeDelimiters;
            return this;
        }

        public DataTableFormatter build() {
            return new DataTableFormatter(rowPrefix, escapeDelimiters);
        }

    }

}
