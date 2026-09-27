package io.cucumber.prettyformatter;

import io.cucumber.messages.types.PickleDocString;
import io.cucumber.messages.types.PickleStepArgument;
import io.cucumber.messages.types.PickleTable;

import java.io.PrintWriter;

final class PickleStepArgumentPrinter {

    private final int indentation;
    private final Theme theme;

    PickleStepArgumentPrinter(Theme theme, int indentation) {
        this.indentation = indentation;
        this.theme = theme;
    }

    void printTo(PickleStepArgument pickleStepArgument, PrintWriter out) {
        var dataTableIndex = pickleStepArgument.getDataTable()
                .flatMap(PickleTable::getArgumentIndex)
                .orElse(-1);
        var docStringIndex  = pickleStepArgument.getDocString()
                .flatMap(PickleDocString::getArgumentIndex)
                .orElse(-1);

        if (dataTableIndex < docStringIndex) {
            printDataTableArgumentTo(pickleStepArgument, out);
            printDocStringArgumentTo(pickleStepArgument, out);
        } else {
            printDocStringArgumentTo(pickleStepArgument, out);
            printDataTableArgumentTo(pickleStepArgument, out);
        }
    }

    private void printDocStringArgumentTo(PickleStepArgument pickleStepArgument, PrintWriter out) {
        pickleStepArgument.getDocString().ifPresent(pickleDocString ->
                out.print(new LineBuilder(theme)
                        .accept(lineBuilder -> PickleDocStringFormatter.builder()
                                .indentation(indentation)
                                .build()
                                .formatTo(pickleDocString, lineBuilder))
                        .build())
        );
    }

    private void printDataTableArgumentTo(PickleStepArgument pickleStepArgument, PrintWriter out) {
        pickleStepArgument.getDataTable().ifPresent(pickleTable ->
                out.print(new LineBuilder(theme)
                        .accept(lineBuilder -> PickleTableFormatter.builder()
                                .indentation(indentation)
                                .build()
                                .formatTo(pickleTable, lineBuilder))
                        .build())
        );
    }
}
