import { Attachment, Envelope, TestCaseStarted, TestStepFinished } from '@cucumber/messages'
import { Query } from '@cucumber/query'

import {
  ATTACHMENT_INDENT_LENGTH,
  ERROR_INDENT_LENGTH,
  formatAttachment,
  formatError,
  formatScenarioLine,
  formatStepArgument,
  formatStepLine,
  formatTags,
  preCalculateMaxContentLength,
  STEP_ARGUMENT_INDENT_LENGTH,
  STEP_INDENT_LENGTH,
} from './helpers.js'

export default {
  type: 'formatter',
  formatter({
    on,
    write,
  }: {
    on: (type: 'message', handler: (message: Envelope) => void) => void
    write: (content: string) => void
  }) {
    const maxContentLengthByTestCaseStartedId = new Map<string, number>()
    const query = new Query()

    on('message', (message: Envelope) => {
      query.update(message)

      if (message.testCaseStarted) {
        const maxContentLength = preCalculateMaxContentLength(message.testCaseStarted, query)
        maxContentLengthByTestCaseStartedId.set(message.testCaseStarted.id, maxContentLength)
        write('\n')
        write(printTestCaseStarted(message.testCaseStarted, query, maxContentLength))
        write('\n')
      }

      if (message.attachment) {
        const output = printAttachment(message.attachment)
        if (output) {
          write(output)
          write('\n')
        }
      }

      if (message.testStepFinished) {
        const maxContentLength =
          maxContentLengthByTestCaseStartedId.get(message.testStepFinished.testCaseStartedId) ?? 0
        const output = printTestStepFinished(message.testStepFinished, query, maxContentLength)
        if (output) {
          write(output)
          write('\n')
        }
      }
    })
  },
  optionsKey: 'pretty',
}

function printTestCaseStarted(
  testCaseStarted: TestCaseStarted,
  query: Query,
  maxContentLength: number
): string {
  return [
    printTags(testCaseStarted, query),
    printScenarioLine(testCaseStarted, query, maxContentLength),
  ]
    .filter((content) => !!content)
    .join('\n')
}

function printTags(testCaseStarted: TestCaseStarted, query: Query): string | undefined {
  return formatTags(testCaseStarted, query)
}

function printScenarioLine(
  testCaseStarted: TestCaseStarted,
  query: Query,
  maxContentLength: number
): string | undefined {
  const formatted = formatScenarioLine(testCaseStarted, query)
  if (!formatted) {
    return undefined
  }
  const [title, location] = formatted
  return printGherkinLine(title, location, maxContentLength)
}

function printTestStepFinished(
  testStepFinished: TestStepFinished,
  query: Query,
  maxContentLength: number
): string {
  return [
    printStepLine(testStepFinished, query, maxContentLength),
    printStepArgument(testStepFinished, query),
    printError(testStepFinished),
  ]
    .filter((content) => !!content)
    .join('\n')
}

function printStepLine(testStepFinished: TestStepFinished, query: Query, maxContentLength: number) {
  const formatted = formatStepLine(testStepFinished, query)
  if (!formatted) {
    return undefined
  }
  const [title, location] = formatted
  const paddedTitle = `${' '.repeat(STEP_INDENT_LENGTH)}${title}`
  return printGherkinLine(paddedTitle, location, maxContentLength)
}

function printStepArgument(testStepFinished: TestStepFinished, query: Query) {
  const content = formatStepArgument(testStepFinished, query)
  if (content) {
    return content
      .split('\n')
      .map((line) => ' '.repeat(STEP_INDENT_LENGTH + STEP_ARGUMENT_INDENT_LENGTH) + line)
      .join('\n')
  }
}

function printGherkinLine(title: string, location: string | undefined, maxContentLength: number) {
  if (location) {
    const padding = maxContentLength - title.length
    return `${title}${' '.repeat(padding)} # ${location}`
  }
  return title
}

function printError(testStepFinished: TestStepFinished): string | undefined {
  const content = formatError(testStepFinished.testStepResult)
  if (content) {
    return content
      .split('\n')
      .map((line) => ' '.repeat(STEP_INDENT_LENGTH + ERROR_INDENT_LENGTH) + line)
      .join('\n')
  }
}

function printAttachment(attachment: Attachment) {
  const content = formatAttachment(attachment)
  return [
    '',
    ...content
      .split('\n')
      .map((line) => ' '.repeat(STEP_INDENT_LENGTH + ATTACHMENT_INDENT_LENGTH) + line),
    '',
  ].join('\n')
}
