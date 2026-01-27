import { TestCaseFinished, TestStepResultStatus } from '@cucumber/messages'
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
import { ensure, GHERKIN_INDENT_LENGTH, indent, join, STEP_ARGUMENT_INDENT_LENGTH } from '../utils'

const ERROR_INDENT_LENGTH = 4
const ATTACHMENT_INDENT_LENGTH = 4

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
  const status =
    query.findMostSevereTestStepResultBy(testCaseFinished)?.status ?? TestStepResultStatus.UNKNOWN
  const [testStepFinished, testStep] = ensure(
    query
      .findTestStepFinishedAndTestStepBy(testCaseStarted)
      .find(([tsf]) => tsf.testStepResult.status === status),
    'Responsible step must exist for non-passing scenario'
  )

  const lines: string[] = []

  const formattedLocation = formatPickleLocation(pickle, location, theme, stream)
  const formattedAttempt =
    testCaseStarted.attempt > 0 ? `, after ${testCaseStarted.attempt + 1} attempts` : ''
  lines.push(`${pickle.name}${formattedAttempt} ${formattedLocation}`)

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

  if (status === TestStepResultStatus.FAILED) {
    const error =
      testStepFinished.testStepResult.exception?.stackTrace ||
      testStepFinished.testStepResult.exception?.message ||
      testStepFinished.testStepResult.message
    if (error) {
      lines.push(
        indent(
          formatError(error, status, theme, stream),
          GHERKIN_INDENT_LENGTH + ERROR_INDENT_LENGTH
        )
      )
    }
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

  return lines.join('\n')
}
