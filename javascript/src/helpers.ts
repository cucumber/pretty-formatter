import { stripVTControlCharacters } from 'node:util'

import {
  Attachment,
  AttachmentContentEncoding,
  Feature,
  Hook,
  HookType,
  Location,
  Pickle,
  PickleDocString,
  PickleStep,
  PickleTable,
  Rule,
  Scenario,
  Step,
  StepDefinition,
  TestRunFinished,
  TestStep,
  TestStepResult,
  TestStepResultStatus,
  TimeConversion,
  Timestamp,
} from '@cucumber/messages'
import { Interval } from 'luxon'

import { TextBuilder } from './TextBuilder'
import { Theme } from './types'

export const GHERKIN_INDENT_LENGTH = 2
export const STEP_ARGUMENT_INDENT_LENGTH = 2
export const ATTACHMENT_INDENT_LENGTH = 4
export const ERROR_INDENT_LENGTH = 4
export const ORDERED_STATUSES: TestStepResultStatus[] = [
  TestStepResultStatus.UNKNOWN,
  TestStepResultStatus.PASSED,
  TestStepResultStatus.SKIPPED,
  TestStepResultStatus.PENDING,
  TestStepResultStatus.UNDEFINED,
  TestStepResultStatus.AMBIGUOUS,
  TestStepResultStatus.FAILED,
]
const STATUS_CHARACTERS: Record<TestStepResultStatus, string> = {
  [TestStepResultStatus.AMBIGUOUS]: 'A',
  [TestStepResultStatus.FAILED]: 'F',
  [TestStepResultStatus.PASSED]: '.',
  [TestStepResultStatus.PENDING]: 'P',
  [TestStepResultStatus.SKIPPED]: '-',
  [TestStepResultStatus.UNDEFINED]: 'U',
  [TestStepResultStatus.UNKNOWN]: '?',
} as const
const HOOK_TYPE_LABELS: Record<HookType, string> = {
  [HookType.BEFORE_TEST_RUN]: 'BeforeTestRun',
  [HookType.AFTER_TEST_RUN]: 'AfterTestRun',
  [HookType.BEFORE_TEST_CASE]: 'Before',
  [HookType.AFTER_TEST_CASE]: 'After',
  [HookType.BEFORE_TEST_STEP]: 'BeforeStep',
  [HookType.AFTER_TEST_STEP]: 'AfterStep',
}
const DURATION_FORMAT = "m'm' s.S's'"

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

export function formatFeatureTitle(feature: Feature, theme: Theme, stream: NodeJS.WritableStream) {
  return new TextBuilder(stream)
    .append(feature.keyword + ':', theme.feature?.keyword)
    .space()
    .append(feature.name, theme.feature?.name)
    .build(theme.feature?.all)
}

export function formatRuleTitle(rule: Rule, theme: Theme, stream: NodeJS.WritableStream) {
  return new TextBuilder(stream)
    .append(rule.keyword + ':', theme.rule?.keyword)
    .space()
    .append(rule.name, theme.rule?.name)
    .build(theme.rule?.all)
}

export function formatPickleTags(pickle: Pickle, theme: Theme, stream: NodeJS.WritableStream) {
  if (pickle && pickle.tags.length > 0) {
    return new TextBuilder(stream)
      .append(pickle.tags.map((tag) => `${tag.name}`).join(' '))
      .build(theme.tag)
  }
}

export function formatPickleTitle(
  pickle: Pickle,
  scenario: Scenario,
  theme: Theme,
  stream: NodeJS.WritableStream
) {
  return new TextBuilder(stream)
    .append(scenario.keyword + ':', theme.scenario?.keyword)
    .space()
    .append(pickle.name || '', theme.scenario?.name)
    .build(theme.scenario?.all)
}

export function formatPickleLocation(
  pickle: Pickle,
  location: Location | undefined,
  theme: Theme,
  stream: NodeJS.WritableStream
) {
  const builder = new TextBuilder(stream).append('#').space().append(pickle.uri)
  if (location) {
    builder.append(':').append(location.line)
  }
  return builder.build(theme.location)
}

export function formatHookTitle(hook: Hook | undefined) {
  let title = ''
  if (hook?.type) {
    title += HOOK_TYPE_LABELS[hook.type]
  } else {
    title += 'Hook'
  }
  if (hook?.name) {
    title += ` (${hook.name})`
  }
  return title
}

export function formatHookLocation(
  hook: Hook | undefined,
  theme: Theme,
  stream: NodeJS.WritableStream
) {
  if (hook?.sourceReference.uri) {
    const builder = new TextBuilder(stream).append('#').space().append(hook.sourceReference.uri)
    if (hook.sourceReference.location) {
      builder.append(':').append(hook.sourceReference.location.line)
    }
    return builder.build(theme.location)
  }
}

export function formatStepTitle(
  testStep: TestStep,
  pickleStep: PickleStep,
  step: Step,
  status: TestStepResultStatus,
  theme: Theme,
  stream: NodeJS.WritableStream
) {
  const builder = new TextBuilder(stream)
  if (theme.status?.icon?.[status]) {
    builder.append(theme.status.icon[status], theme.status?.all?.[status]).space()
  }
  return builder
    .append(
      new TextBuilder(stream)
        .append(step.keyword, theme.step?.keyword)
        // step keyword includes a trailing space
        .append(formatStepText(testStep, pickleStep, theme, stream))
        .build(theme.status?.all?.[status])
    )
    .build()
}

function formatStepText(
  testStep: TestStep,
  pickleStep: PickleStep,
  theme: Theme,
  stream: NodeJS.WritableStream
) {
  const builder = new TextBuilder(stream)
  const stepMatchArgumentsLists = testStep.stepMatchArgumentsLists
  if (stepMatchArgumentsLists && stepMatchArgumentsLists.length === 1) {
    const stepMatchArguments = stepMatchArgumentsLists[0].stepMatchArguments
    let currentIndex = 0
    stepMatchArguments.forEach((argument) => {
      const group = argument.group
      // Ignore absent values, or groups without a start
      if (group.value !== undefined && group.start !== undefined) {
        const text = pickleStep.text.slice(currentIndex, group.start)
        currentIndex = group.start + group.value.length
        builder.append(text, theme.step?.text).append(group.value, theme.step?.argument)
      }
    })
    if (currentIndex != pickleStep.text.length) {
      const remainder = pickleStep.text.slice(currentIndex)
      builder.append(remainder, theme.step?.text)
    }
  } else {
    builder.append(pickleStep.text, theme.step?.text)
  }
  return builder.build()
}

export function formatStepLocation(
  stepDefinition: StepDefinition | undefined,
  theme: Theme,
  stream: NodeJS.WritableStream
) {
  if (stepDefinition?.sourceReference.uri) {
    const builder = new TextBuilder(stream)
      .append('#')
      .space()
      .append(stepDefinition.sourceReference.uri)
    if (stepDefinition.sourceReference.location) {
      builder.append(':').append(stepDefinition.sourceReference.location.line)
    }
    return builder.build(theme.location)
  }
}

export function formatStepArgument(
  pickleStep: PickleStep,
  theme: Theme,
  stream: NodeJS.WritableStream
) {
  if (pickleStep.argument?.docString) {
    return formatDocString(pickleStep.argument.docString, theme, stream)
  }
  if (pickleStep.argument?.dataTable) {
    return formatDataTable(pickleStep.argument.dataTable, theme, stream)
  }
}

function formatDocString(docString: PickleDocString, theme: Theme, stream: NodeJS.WritableStream) {
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

function formatDataTable(dataTable: PickleTable, theme: Theme, stream: NodeJS.WritableStream) {
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

export function formatTestStepResultError(
  testStepResult: TestStepResult,
  theme: Theme,
  stream: NodeJS.WritableStream
): string | undefined {
  if (testStepResult.exception?.stackTrace) {
    return new TextBuilder(stream)
      .append(testStepResult.exception.stackTrace.trim())
      .build(theme.status?.all?.[testStepResult.status], true)
  }
  // Fallback
  if (testStepResult.exception?.message) {
    return new TextBuilder(stream)
      .append(testStepResult.exception.message.trim())
      .build(theme.status?.all?.[testStepResult.status], true)
  }
  // Fallback
  if (testStepResult.message) {
    return new TextBuilder(stream)
      .append(testStepResult.message.trim())
      .build(theme.status?.all?.[testStepResult.status], true)
  }
}

export function formatTestRunFinishedError(
  testRunFinished: TestRunFinished,
  theme: Theme,
  stream: NodeJS.WritableStream
) {
  if (testRunFinished.exception?.stackTrace) {
    return new TextBuilder(stream)
      .append(testRunFinished.exception.stackTrace.trim())
      .build(theme.status?.all?.[TestStepResultStatus.FAILED], true)
  }
  // Fallback
  if (testRunFinished.exception?.message) {
    return new TextBuilder(stream)
      .append(testRunFinished.exception.message.trim())
      .build(theme.status?.all?.[TestStepResultStatus.FAILED], true)
  }
}

export function formatAttachment(
  attachment: Attachment,
  theme: Theme,
  stream: NodeJS.WritableStream
) {
  switch (attachment.contentEncoding) {
    case AttachmentContentEncoding.BASE64:
      return formatBase64Attachment(
        attachment.body,
        attachment.mediaType,
        attachment.fileName,
        theme,
        stream
      )
    case AttachmentContentEncoding.IDENTITY:
      return formatTextAttachment(attachment.body, theme, stream)
  }
}

function formatBase64Attachment(
  data: string,
  mediaType: string,
  fileName: string | undefined,
  theme: Theme,
  stream: NodeJS.WritableStream
) {
  const builder = new TextBuilder(stream)
  const bytes = (data.length / 4) * 3
  if (fileName) {
    builder.append(`Embedding ${fileName} [${mediaType} ${bytes} bytes]`)
  } else {
    builder.append(`Embedding [${mediaType} ${bytes} bytes]`)
  }
  return builder.build(theme.attachment)
}

function formatTextAttachment(content: string, theme: Theme, stream: NodeJS.WritableStream) {
  return new TextBuilder(stream).append(content).build(theme.attachment)
}

export function formatStatusCharacter(
  status: TestStepResultStatus,
  theme: Theme,
  stream: NodeJS.WritableStream
) {
  const character = STATUS_CHARACTERS[status]
  return new TextBuilder(stream).append(character).build(theme.status?.all?.[status])
}

export function formatForStatus(
  status: TestStepResultStatus,
  text: string,
  theme: Theme,
  stream: NodeJS.WritableStream
) {
  return new TextBuilder(stream).append(text).build(theme.status?.all?.[status])
}

export function formatCounts(
  suffix: string,
  counts: Partial<Record<TestStepResultStatus, number>>,
  theme: Theme,
  stream: NodeJS.WritableStream
) {
  const builder = new TextBuilder(stream)
  const total = Object.values(counts).reduce((prev, curr) => prev + curr, 0)
  builder.append(`${total} ${suffix}`)
  if (total > 0) {
    let first = true
    builder.append(' (')
    for (const status of ORDERED_STATUSES) {
      const count = counts[status]
      if (count) {
        if (!first) {
          builder.append(', ')
        }
        builder.append(`${count} ${status.toLowerCase()}`, theme.status?.all?.[status])
        first = false
      }
    }
    builder.append(')')
  }
  return builder.build()
}

export function formatDuration(start: Timestamp, finish: Timestamp) {
  const startMillis = new Date(TimeConversion.timestampToMillisecondsSinceEpoch(start))
  const finishMillis = new Date(TimeConversion.timestampToMillisecondsSinceEpoch(finish))
  const duration = Interval.fromDateTimes(startMillis, finishMillis).toDuration([
    'minutes',
    'seconds',
    'milliseconds',
  ])
  return duration.toFormat(DURATION_FORMAT)
}

export function titleCaseStatus(status: TestStepResultStatus) {
  return `${status.charAt(0).toUpperCase() + status.slice(1).toLowerCase()}`
}
