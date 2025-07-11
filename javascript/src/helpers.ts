import {
  Attachment,
  AttachmentContentEncoding,
  Location,
  Pickle,
  PickleDocString,
  PickleStep,
  PickleTable,
  Scenario,
  Step,
  StepDefinition,
  TestStepResult,
} from '@cucumber/messages'

export const STEP_INDENT_LENGTH = 2
export const STEP_ARGUMENT_INDENT_LENGTH = 2
export const ATTACHMENT_INDENT_LENGTH = 4
export const ERROR_INDENT_LENGTH = 4

export function ensure<T>(value: T | undefined, message: string): T {
  if (!value) {
    throw new Error(message)
  }
  return value
}

export function indent(original: string, by: number) {
  return original
    .trim()
    .split('\n')
    .map((line) => ' '.repeat(by) + line)
    .join('\n')
}

export function pad(original: string) {
  return `\n` + original + '\n'
}

export function formatPickleTags(pickle: Pickle) {
  if (pickle && pickle.tags.length > 0) {
    return pickle.tags.map((tag) => `${tag.name}`).join(' ')
  }
}
export function formatPickleTitle(pickle: Pickle, scenario: Scenario) {
  return `${scenario.keyword}: ${pickle.name || ''}`
}

export function formatPickleLocation(pickle: Pickle, location: Location | undefined) {
  if (location) {
    return `${pickle.uri}:${location.line}`
  }
  return pickle.uri
}

export function formatStepTitle(pickleStep: PickleStep, step: Step) {
  // step keyword includes a trailing space
  return `${step.keyword}${pickleStep.text}`
}

export function formatStepLocation(stepDefinition: StepDefinition | undefined) {
  if (stepDefinition) {
    let output = stepDefinition.sourceReference.uri
    if (stepDefinition.sourceReference.location) {
      output += `:${stepDefinition.sourceReference.location.line}`
    }
    return output
  }
}

export function formatStepArgument(pickleStep: PickleStep) {
  if (pickleStep.argument?.docString) {
    return formatDocString(pickleStep.argument.docString)
  }
  if (pickleStep.argument?.dataTable) {
    return formatDataTable(pickleStep.argument.dataTable)
  }
}

function formatDocString(docString: PickleDocString) {
  return `"""${docString.mediaType ?? ''}
${docString.content}
"""`
}

function formatDataTable(dataTable: PickleTable) {
  const columnWidths: number[] = []

  for (const row of dataTable.rows) {
    for (let i = 0; i < row.cells.length; i++) {
      const cellWidth = row.cells[i].value.length
      if (columnWidths[i] === undefined || cellWidth > columnWidths[i]) {
        columnWidths[i] = cellWidth
      }
    }
  }

  return dataTable.rows
    .map((row) => {
      return (
        '| ' + row.cells.map((cell, i) => cell.value.padEnd(columnWidths[i])).join(' | ') + ' |'
      )
    })
    .join('\n')
}

export function formatError(testStepResult: TestStepResult): string | undefined {
  return testStepResult.exception?.stackTrace || testStepResult.exception?.message
}

export function formatAttachment(attachment: Attachment) {
  switch (attachment.contentEncoding) {
    case AttachmentContentEncoding.BASE64:
      return formatBase64Attachment(attachment.body, attachment.mediaType, attachment.fileName)
    case AttachmentContentEncoding.IDENTITY:
      return formatTextAttachment(attachment.body)
  }
}

function formatBase64Attachment(data: string, mediaType: string, fileName?: string) {
  const bytes = (data.length / 4) * 3
  if (fileName) {
    return `Embedding ${fileName} [${mediaType} ${bytes} bytes]`
  } else {
    return `Embedding [${mediaType} ${bytes} bytes]`
  }
}

function formatTextAttachment(content: string) {
  return content
}
