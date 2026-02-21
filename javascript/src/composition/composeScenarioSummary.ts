import {
  TestCaseFinished,
  TestStep,
  TestStepFinished,
  TestStepResultStatus,
} from '@cucumber/messages'
import { Query } from '@cucumber/query'

import {
  formatAmbiguousStep,
  formatAttachment,
  formatError,
  formatHookTitle,
  formatPickleLocation,
  formatPickleStepArgument,
  formatSourceReference,
  formatStepTitle,
} from '../formatting'
import type { SummaryOptions } from '../types'
import {
  ensure,
  extractReportableMessage,
  GHERKIN_INDENT_LENGTH,
  indent,
  join,
  STEP_ARGUMENT_INDENT_LENGTH,
} from '../utils'

const ERROR_INDENT_LENGTH = 4
const ATTACHMENT_INDENT_LENGTH = 4

/**
 * Finds all pertinent (non-passing) steps that should be shown in the summary.
 * Returns the first non-passed step, plus every subsequent step that is not passed or skipped.
 */
function findPertinentSteps(
  stepsWithFinished: ReadonlyArray<readonly [TestStepFinished, TestStep]>
): Array<[TestStepFinished, TestStep]> {
  const result: Array<[TestStepFinished, TestStep]> = []
  let foundFirstNonPassed = false

  for (const [testStepFinished, testStep] of stepsWithFinished) {
    const status = testStepFinished.testStepResult.status
    if (!foundFirstNonPassed) {
      if (status !== TestStepResultStatus.PASSED) {
        result.push([testStepFinished, testStep])
        foundFirstNonPassed = true
      }
    } else {
      if (status !== TestStepResultStatus.PASSED && status !== TestStepResultStatus.SKIPPED) {
        result.push([testStepFinished, testStep])
      }
    }
  }
  return result
}

interface FormatStepContext {
  query: Query
  theme: Required<SummaryOptions>['theme']
  stream: NodeJS.WritableStream
  includeAttachments: boolean
}

/**
 * Formats a single step for the summary output.
 */
function formatStep(
  testStepFinished: TestStepFinished,
  testStep: TestStep,
  context: FormatStepContext
): string[] {
  const { query, theme, stream, includeAttachments } = context
  const lines: string[] = []
  const status = testStepFinished.testStepResult.status

  if (testStep.pickleStepId) {
    const pickleStep = ensure(
      query.findPickleStepBy(testStep),
      'PickleStep must exist for Step with pickleStepId'
    )
    const step = ensure(query.findStepBy(pickleStep), 'Step must exist for PickleStep')
    const stepDefinition = query.findUnambiguousStepDefinitionBy(testStep)

    lines.push(
      indent(
        join(
          formatStepTitle(testStep, pickleStep, step, status, false, theme, stream),
          stepDefinition?.sourceReference
            ? formatSourceReference(stepDefinition.sourceReference, theme, stream)
            : undefined
        ),
        GHERKIN_INDENT_LENGTH
      )
    )
    if (pickleStep.argument) {
      lines.push(
        indent(
          formatPickleStepArgument(pickleStep.argument, theme, stream),
          GHERKIN_INDENT_LENGTH + STEP_ARGUMENT_INDENT_LENGTH
        )
      )
    }
    if (status === TestStepResultStatus.AMBIGUOUS) {
      const stepDefinitions = query.findStepDefinitionsBy(testStep)
      lines.push(
        indent(
          formatAmbiguousStep(stepDefinitions, theme, stream),
          GHERKIN_INDENT_LENGTH + ERROR_INDENT_LENGTH
        )
      )
    }
  } else if (testStep.hookId) {
    const hook = query.findHookBy(testStep)
    lines.push(
      indent(
        join(
          formatHookTitle(hook, status, theme, stream),
          hook?.sourceReference
            ? formatSourceReference(hook.sourceReference, theme, stream)
            : undefined
        ),
        GHERKIN_INDENT_LENGTH
      )
    )
  }

  const error = extractReportableMessage(testStepFinished.testStepResult)
  if (error) {
    lines.push(
      indent(formatError(error, status, theme, stream), GHERKIN_INDENT_LENGTH + ERROR_INDENT_LENGTH)
    )
  }

  if (includeAttachments) {
    const attachments = query.findAttachmentsBy(testStepFinished)
    attachments.forEach((attachment) => {
      lines.push('')
      lines.push(
        indent(
          formatAttachment(attachment, theme, stream),
          GHERKIN_INDENT_LENGTH + ATTACHMENT_INDENT_LENGTH
        )
      )
    })
  }

  return lines
}

export function composeScenarioSummary(
  testCaseFinished: TestCaseFinished,
  query: Query,
  { includeAttachments, theme }: Required<SummaryOptions>,
  stream: NodeJS.WritableStream
): string {
  const testCaseStarted = ensure(
    query.findTestCaseStartedBy(testCaseFinished),
    'TestCaseStarted must exist for TestCaseFinished'
  )
  const pickle = ensure(
    query.findPickleBy(testCaseFinished),
    'Pickle must exist for TestCaseFinished'
  )
  const location = query.findLocationOf(pickle)

  const allSteps = query.findTestStepFinishedAndTestStepBy(testCaseStarted)
  const pertinentSteps = findPertinentSteps(allSteps)

  const lines: string[] = []

  const formattedLocation = formatPickleLocation(pickle, location, theme, stream)
  const formattedAttempt =
    testCaseStarted.attempt > 0 ? `, after ${testCaseStarted.attempt + 1} attempts` : ''
  lines.push(`${pickle.name}${formattedAttempt} ${formattedLocation}`)

  const context: FormatStepContext = { query, theme, stream, includeAttachments }
  for (const [testStepFinished, testStep] of pertinentSteps) {
    lines.push(...formatStep(testStepFinished, testStep, context))
  }

  return lines.join('\n')
}
