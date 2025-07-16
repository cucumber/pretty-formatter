import { stripVTControlCharacters } from 'node:util'

import {
  Attachment,
  AttachmentContentEncoding,
  Feature,
  Location,
  Pickle,
  PickleDocString,
  PickleStep,
  PickleTable,
  Rule,
  Scenario,
  Step,
  StepDefinition,
  TestStep,
  TestStepResult,
  TestStepResultStatus,
} from '@cucumber/messages'

import { TextBuilder } from './TextBuilder.js'
import { Theme } from './types.js'

export const GHERKIN_INDENT_LENGTH = 2
export const STEP_ARGUMENT_INDENT_LENGTH = 2
export const ATTACHMENT_INDENT_LENGTH = 4
export const ERROR_INDENT_LENGTH = 4

const ICON_BY_STATUS: Record<TestStepResultStatus, string> = {
  [TestStepResultStatus.AMBIGUOUS]: '✘',
  [TestStepResultStatus.FAILED]: '✘',
  [TestStepResultStatus.PASSED]: '✔',
  [TestStepResultStatus.PENDING]: '■',
  [TestStepResultStatus.SKIPPED]: '↷',
  [TestStepResultStatus.UNDEFINED]: '■',
  [TestStepResultStatus.UNKNOWN]: ' ',
}

export function ensure<T>(value: T | undefined, message: string): T {
  if (!value) {
    throw new Error(message)
  }
  return value
}

export function indent(original: string, by: number) {
  return original
    .split('\n')
    .map((line) => ' '.repeat(by) + line)
    .join('\n')
}

export function pad(original: string) {
  return `\n` + original + '\n'
}

export function unstyled(text: string) {
  return stripVTControlCharacters(text)
}

export function formatFeatureTitle(feature: Feature, theme: Theme) {
  return new TextBuilder()
    .append(feature.keyword + ':', theme.feature?.keyword)
    .space()
    .append(feature.name, theme.feature?.name)
    .build(theme.feature?.all)
}

export function formatRuleTitle(rule: Rule, theme: Theme) {
  return new TextBuilder()
    .append(rule.keyword + ':', theme.rule?.keyword)
    .space()
    .append(rule.name, theme.rule?.name)
    .build(theme.rule?.all)
}

export function formatPickleTags(pickle: Pickle, theme: Theme) {
  if (pickle && pickle.tags.length > 0) {
    return new TextBuilder()
      .append(pickle.tags.map((tag) => `${tag.name}`).join(' '))
      .build(theme.tag)
  }
}
export function formatPickleTitle(pickle: Pickle, scenario: Scenario, theme: Theme) {
  return new TextBuilder()
    .append(scenario.keyword + ':', theme.scenario?.keyword)
    .space()
    .append(pickle.name || '', theme.scenario?.name)
    .build(theme.scenario?.all)
}

export function formatPickleLocation(pickle: Pickle, location: Location | undefined, theme: Theme) {
  const builder = new TextBuilder().append('#').space().append(pickle.uri)
  if (location) {
    builder.append(':').append(location.line)
  }
  return builder.build(theme.location)
}

export function formatStepTitle(
  testStep: TestStep,
  pickleStep: PickleStep,
  step: Step,
  status: TestStepResultStatus,
  statusIcon: boolean = false,
  theme: Theme
) {
  const builder = new TextBuilder()
  if (statusIcon) {
    builder.append(ICON_BY_STATUS[status], theme.status?.[status]).space()
  }
  return builder
    .append(
      new TextBuilder()
        .append(step.keyword, theme.step?.keyword)
        // step keyword includes a trailing space
        .append(formatStepText(testStep, pickleStep, theme))
        .build(theme.status?.[status])
    )
    .build()
}

function formatStepText(testStep: TestStep, pickleStep: PickleStep, theme: Theme) {
  const builder = new TextBuilder()
  const stepMatchArgumentsLists = testStep.stepMatchArgumentsLists
  if (stepMatchArgumentsLists && stepMatchArgumentsLists.length === 1) {
    const stepMatchArguments = stepMatchArgumentsLists[0].stepMatchArguments
    let offset = 0
    let plain: string
    stepMatchArguments.forEach((argument) => {
      plain = pickleStep.text.slice(offset, argument.group.start)
      builder.append(plain, theme.step?.text)
      const arg = argument.group.value
      if (arg) {
        if (arg.length > 0) {
          builder.append(arg, theme.step?.argument)
        }
        offset += plain.length + arg.length
      }
    })
    plain = pickleStep.text.slice(offset)
    if (plain.length > 0) {
      builder.append(plain, theme.step?.text)
    }
  } else {
    builder.append(pickleStep.text, theme.step?.text)
  }
  return builder.build()
}

export function formatStepLocation(stepDefinition: StepDefinition | undefined, theme: Theme) {
  if (stepDefinition?.sourceReference.uri) {
    const builder = new TextBuilder().append('#').space().append(stepDefinition.sourceReference.uri)
    if (stepDefinition.sourceReference.location) {
      builder.append(':').append(stepDefinition.sourceReference.location.line)
    }
    return builder.build(theme.location)
  }
}

export function formatStepArgument(pickleStep: PickleStep, theme: Theme) {
  if (pickleStep.argument?.docString) {
    return formatDocString(pickleStep.argument.docString, theme)
  }
  if (pickleStep.argument?.dataTable) {
    return formatDataTable(pickleStep.argument.dataTable, theme)
  }
}

function formatDocString(docString: PickleDocString, theme: Theme) {
  const builder = new TextBuilder().append('"""', theme.docString?.delimiter)
  if (docString.mediaType) {
    builder.append(docString.mediaType, theme.docString?.mediaType)
  }
  builder
    .line()
    .append(docString.content, theme.docString?.content)
    .line()
    .append('"""', theme.docString?.delimiter)
  return builder.build(theme.docString?.all, true)
}

function formatDataTable(dataTable: PickleTable, theme: Theme) {
  const columnWidths = calculateColumnWidths(dataTable)
  const builder = new TextBuilder()
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

export function formatError(testStepResult: TestStepResult, theme: Theme): string | undefined {
  const error = (testStepResult.exception?.stackTrace || testStepResult.exception?.message)?.trim()
  if (error) {
    return new TextBuilder().append(error.trim()).build(theme.status?.[testStepResult.status], true)
  }
}

export function formatAttachment(attachment: Attachment, theme: Theme) {
  switch (attachment.contentEncoding) {
    case AttachmentContentEncoding.BASE64:
      return formatBase64Attachment(
        attachment.body,
        attachment.mediaType,
        attachment.fileName,
        theme
      )
    case AttachmentContentEncoding.IDENTITY:
      return formatTextAttachment(attachment.body, theme)
  }
}

function formatBase64Attachment(
  data: string,
  mediaType: string,
  fileName: string | undefined,
  theme: Theme
) {
  const builder = new TextBuilder()
  const bytes = (data.length / 4) * 3
  if (fileName) {
    builder.append(`Embedding ${fileName} [${mediaType} ${bytes} bytes]`)
  } else {
    builder.append(`Embedding [${mediaType} ${bytes} bytes]`)
  }
  return builder.build(theme.attachment)
}

function formatTextAttachment(content: string, theme: Theme) {
  return new TextBuilder().append(content).build(theme.attachment)
}
