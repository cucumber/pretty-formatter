import { Envelope, TestCaseStarted, TestStepFinished } from '@cucumber/messages'
import { Query } from '@cucumber/query'

import {
  formatPickleLocation,
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
        write(formatTestCaseStarted(message.testCaseStarted, query, maxContentLength))
        write('\n')
      }

      if (message.testStepFinished) {
        const maxContentLength =
          maxContentLengthByTestCaseStartedId.get(message.testStepFinished.testCaseStartedId) ?? 0
        const output = formatTestStepFinished(message.testStepFinished, query, maxContentLength)
        if (output) {
          write(output)
          write('\n')
        }
      }
    })
  },
  optionsKey: 'pretty',
}

function formatTestCaseStarted(
  testCaseStarted: TestCaseStarted,
  query: Query,
  maxContentLength: number
): string {
  return withScenario(
    testCaseStarted,
    query,
    ({ pickle, scenario }) => {
      const outputs: string[] = []

      if (pickle.tags.length > 0) {
        outputs.push(pickle.tags.map((tag) => `${tag.name}`).join(' '))
      }

      const lineContent = `${scenario.keyword}: ${scenario.name || ''}`
      const padding = maxContentLength - lineContent.length
      const location = formatPickleLocation(pickle, query)
      outputs.push(`${lineContent}${' '.repeat(padding)} # ${location}`)

      return outputs.join('\n')
    },
    ''
  )
}

function formatTestStepFinished(
  testStepFinished: TestStepFinished,
  query: Query,
  maxContentLength: number
): string {
  return withStep(
    testStepFinished,
    query,
    ({ testStep, pickleStep, step }) => {
      const lineContent = `  ${step.keyword}${pickleStep.text}`

      const stepDefinition = query.findUnambiguousStepDefinitionBy(testStep)
      if (stepDefinition) {
        const padding = maxContentLength - lineContent.length
        const location = `${stepDefinition.sourceReference.uri}:${stepDefinition.sourceReference.location?.line ?? 0}`
        return `${lineContent}${' '.repeat(padding)} # ${location}`
      }

      return lineContent
    },
    ''
  )
}
