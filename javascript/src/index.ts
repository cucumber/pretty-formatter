import { Envelope, TestCaseStarted, TestStepFinished } from '@cucumber/messages'
import { Query } from '@cucumber/query'

import {
  formatError,
  formatPickleLocation,
  formatStepLocation,
  preCalculateMaxContentLength,
  withScenario,
  withStep,
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
    printScenario(testCaseStarted, query, maxContentLength),
  ]
    .filter((content) => !!content)
    .join('\n')
}

function printTags(testCaseStarted: TestCaseStarted, query: Query): string | undefined {
    const pickle = query.findPickleBy(testCaseStarted)
    if (pickle && pickle.tags.length > 0) {
        return pickle.tags.map((tag) => `${tag.name}`).join(' ')
    }
}

function printScenario(
  testCaseStarted: TestCaseStarted,
  query: Query,
  maxContentLength: number
): string | undefined {
  return withScenario(
    testCaseStarted,
    query,
    ({ pickle, scenario }) => {
      const lineContent = `${scenario.keyword}: ${pickle.name || ''}`
      const padding = maxContentLength - lineContent.length
      const location = formatPickleLocation(pickle, query)
      return `${lineContent}${' '.repeat(padding)} # ${location}`
    },
    ''
  )
}

function printTestStepFinished(
  testStepFinished: TestStepFinished,
  query: Query,
  maxContentLength: number
): string {
  return [printStep(testStepFinished, query, maxContentLength), printError(testStepFinished)]
    .filter((content) => !!content)
    .join('\n')
}

function printStep(testStepFinished: TestStepFinished, query: Query, maxContentLength: number) {
  return withStep(
    testStepFinished,
    query,
    ({ testStep, pickleStep, step }) => {
      const lineContent = `  ${step.keyword}${pickleStep.text}`
      const padding = maxContentLength - lineContent.length
      const location = formatStepLocation(testStep, query)
      if (location) {
        return `${lineContent}${' '.repeat(padding)} # ${location}`
      }
      return lineContent
    },
    ''
  )
}

function printError(testStepFinished: TestStepFinished) {
  const exception = testStepFinished.testStepResult.exception
  if (exception) {
    const content = exception.stackTrace || exception.message
    if (content) {
      return formatError(content)
    }
  }
}
