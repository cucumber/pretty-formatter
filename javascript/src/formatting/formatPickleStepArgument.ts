import { PickleDocString, PickleStepArgument, PickleTable } from '@cucumber/messages'

import { TextBuilder } from '../TextBuilder'
import { Theme } from '../types'

export function formatPickleStepArgument(
  pickleStepArgument: PickleStepArgument,
  theme: Theme,
  stream: NodeJS.WritableStream
): string {
  if (pickleStepArgument.docString) {
    return formatDocString(pickleStepArgument.docString, theme, stream)
  }
  if (pickleStepArgument.dataTable) {
    return formatDataTable(pickleStepArgument.dataTable, theme, stream)
  }
  throw new Error('PickleStepArgument must have one of dataTable or docString')
}

function formatDocString(
  docString: PickleDocString,
  theme: Theme,
  stream: NodeJS.WritableStream
): string {
  const builder = new TextBuilder(stream).append('"""', theme.docString?.delimiter)
  if (docString.mediaType) {
    builder.append(docString.mediaType, theme.docString?.mediaType)
  }
  builder.line()
  // Doc strings are normalized to \n by Gherkin.
  const lines = docString.content.split('\n')
  lines.forEach((line) => {
    builder.append(line, theme.docString?.content).line()
  })
  builder.append('"""', theme.docString?.delimiter)
  return builder.build(theme.docString?.all, true)
}

function formatDataTable(
  dataTable: PickleTable,
  theme: Theme,
  stream: NodeJS.WritableStream
): string {
  const columnWidths = calculateColumnWidths(dataTable)
  const builder = new TextBuilder(stream)
  dataTable.rows.forEach((row, rowIndex) => {
    if (rowIndex > 0) {
      builder.line()
    }
    builder.append('|', theme.dataTable?.border)
    row.cells.forEach((cell, cellIndex) => {
      builder
        .append(' ' + cell.value.padEnd(columnWidths[cellIndex]) + ' ', theme.dataTable?.content)
        .append('|', theme.dataTable?.border)
    })
  })
  return builder.build(theme.dataTable?.all, true)
}

function calculateColumnWidths(dataTable: PickleTable) {
  const columnWidths: number[] = []
  for (const row of dataTable.rows) {
    for (let i = 0; i < row.cells.length; i++) {
      const cellWidth = row.cells[i].value.length
      if (columnWidths[i] === undefined || cellWidth > columnWidths[i]) {
        columnWidths[i] = cellWidth
      }
    }
  }
  return columnWidths
}
