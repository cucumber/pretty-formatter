import { stripVTControlCharacters } from 'node:util'

import {
  Attachment,
  AttachmentContentEncoding,
  Duration,
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
} from '@cucumber/messages'
import { Duration as LuxonDuration } from 'luxon'

import { TextBuilder } from './TextBuilder'
import { Style, Theme } from './types'

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
export const DEFAULT_STATUS_COLORS: Record<TestStepResultStatus, Style> = {
  [TestStepResultStatus.AMBIGUOUS]: 'red',
  [TestStepResultStatus.FAILED]: 'red',
  [TestStepResultStatus.PASSED]: 'green',
  [TestStepResultStatus.PENDING]: 'yellow',
  [TestStepResultStatus.SKIPPED]: 'cyan',
  [TestStepResultStatus.UNDEFINED]: 'yellow',
  [TestStepResultStatus.UNKNOWN]: 'gray',
}

export const DEFAULT_STATUS_ICONS: Record<TestStepResultStatus, string> = {
  [TestStepResultStatus.AMBIGUOUS]: '✘',
  [TestStepResultStatus.FAILED]: '✘',
  [TestStepResultStatus.PASSED]: '✔',
  [TestStepResultStatus.PENDING]: '■',
  [TestStepResultStatus.SKIPPED]: '↷',
  [TestStepResultStatus.UNDEFINED]: '■',
  [TestStepResultStatus.UNKNOWN]: ' ',
} as const
export const DEFAULT_PROGRESS_ICONS: Record<TestStepResultStatus, string> = {
  [TestStepResultStatus.AMBIGUOUS]: 'A',
  [TestStepResultStatus.FAILED]: 'F',
  [TestStepResultStatus.PASSED]: '.',
  [TestStepResultStatus.PENDING]: 'P',
  [TestStepResultStatus.SKIPPED]: '-',
  [TestStepResultStatus.UNDEFINED]: 'U',
  [TestStepResultStatus.UNKNOWN]: '?',
} as const
const HOOK_TYPE_LABELS: Record<HookType, string> = {
  [HookType.BEFORE_TEST_RUN]: 'BeforeAll',
  [HookType.AFTER_TEST_RUN]: 'AfterAll',
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

export function join(...originals: ReadonlyArray<string | undefined>) {
  return originals.filter((part) => !!part).join(' ')
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

export function formatHookTitle(
  hook: Hook | undefined,
  status: TestStepResultStatus,
  theme: Theme,
  stream: NodeJS.WritableStream
) {
  const builder = new TextBuilder(stream).append(
    hook?.type ? HOOK_TYPE_LABELS[hook.type] : 'Hook',
    theme.step?.keyword
  )
  if (hook?.name) {
    builder.append(` (${hook.name})`, theme.step?.text)
  }
  return builder.build(theme.status?.all?.[status])
}

export function formatStepTitle(
  testStep: TestStep,
  pickleStep: PickleStep,
  step: Step,
  status: TestStepResultStatus,
  useStatusIcon: boolean,
  theme: Theme,
  stream: NodeJS.WritableStream
) {
  const builder = new TextBuilder(stream)
  if (useStatusIcon) {
    builder
      .append(
        theme.status?.icon?.[status] ?? DEFAULT_STATUS_ICONS[status],
        theme.status?.all?.[status]
      )
      .space()
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

export function formatCodeLocation(
  hookOrStepDefinition: Hook | StepDefinition | undefined,
  theme: Theme,
  stream: NodeJS.WritableStream
) {
  if (hookOrStepDefinition?.sourceReference.uri) {
    const builder = new TextBuilder(stream)
      .append('#')
      .space()
      .append(hookOrStepDefinition.sourceReference.uri)
    if (hookOrStepDefinition.sourceReference.location) {
      builder.append(':').append(hookOrStepDefinition.sourceReference.location.line)
    }
    return builder.build(theme.location)
  }
}

export function formatPickleStepArgument(
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

export function formatAmbiguousStep(
  stepDefinitions: readonly StepDefinition[],
  theme: Theme,
  stream: NodeJS.WritableStream
): string | undefined {
  const builder = new TextBuilder(stream)
  builder.append('Multiple matching step definitions found:')
  for (const stepDefinition of stepDefinitions) {
    builder.line()
    builder.append('  - ')
    if (stepDefinition.pattern?.source) {
      builder.append(stepDefinition.pattern.source)
    }
    const location = formatCodeLocation(stepDefinition, theme, stream)
    if (location) {
      builder.space().append(location)
    }
  }
  return builder.build(undefined, true)
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
  const character = DEFAULT_PROGRESS_ICONS[status]
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

export function formatDurations(
  testRunDuration: Duration,
  executionDurations: ReadonlyArray<Duration>
) {
  const testRunLuxon = LuxonDuration.fromMillis(
    TimeConversion.durationToMilliseconds(testRunDuration)
  )

  const executionLuxon = LuxonDuration.fromMillis(
    executionDurations.reduce((prev, curr) => prev + TimeConversion.durationToMilliseconds(curr), 0)
  )

  return `${testRunLuxon.toFormat(DURATION_FORMAT)} (${executionLuxon.toFormat(DURATION_FORMAT)} executing your code)`
}

export function titleCaseStatus(status: TestStepResultStatus) {
  return `${status.charAt(0).toUpperCase() + status.slice(1).toLowerCase()}`
}
