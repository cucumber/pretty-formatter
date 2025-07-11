import {
  Attachment,
  AttachmentContentEncoding,
  Pickle,
  PickleDocString,
  PickleStep,
  Scenario,
  Step,
  TestCaseStarted,
  TestStep,
  TestStepFinished,
  TestStepResult,
} from '@cucumber/messages'
import { Query } from '@cucumber/query'

export const STEP_INDENT_LENGTH = 2
export const STEP_ARGUMENT_INDENT_LENGTH = 2
export const ATTACHMENT_INDENT_LENGTH = 4
export const ERROR_INDENT_LENGTH = 4

export function preCalculateMaxContentLength(
  testCaseStarted: TestCaseStarted,
  query: Query
): number {
  return withScenario(
    testCaseStarted,
    query,
    ({ pickle, scenario }) => {
      const scenarioLength = scenario.keyword.length + ': '.length + pickle.name.length
      const stepLengths = pickle.steps.map((pickleStep) => {
        const step = query.findStepBy(pickleStep)
        return STEP_INDENT_LENGTH + (step?.keyword.length ?? 0) + pickleStep.text.length
      })
      return Math.max(scenarioLength, ...stepLengths)
    },
    0
  )
}

export function formatTags(testCaseStarted: TestCaseStarted, query: Query): string | undefined {
  const pickle = query.findPickleBy(testCaseStarted)
  if (pickle && pickle.tags.length > 0) {
    return pickle.tags.map((tag) => `${tag.name}`).join(' ')
  }
}

export function formatScenarioLine(
  testCaseStarted: TestCaseStarted,
  query: Query
): [string, string | undefined] | undefined {
  return withScenario(
    testCaseStarted,
    query,
    ({ pickle, scenario }) => {
      const title = `${scenario.keyword}: ${pickle.name || ''}`
      const location = formatPickleLocation(pickle, query)
      return [title, location]
    },
    undefined
  )
}

function formatPickleLocation(pickle: Pickle, query: Query): string | undefined {
  let result = pickle.uri
  const location = query.findLocationOf(pickle)
  if (location) {
    result += `:${location.line}`
  }
  return result
}

export function formatStepLine(
  testStepFinished: TestStepFinished,
  query: Query
): [string, string | undefined] | undefined {
  return withStep(
    testStepFinished,
    query,
    ({ testStep, pickleStep, step }) => {
      const title = `${step.keyword}${pickleStep.text}`
      const location = formatStepLocation(testStep, query)
      return [title, location]
    },
    undefined
  )
}

function formatStepLocation(testStep: TestStep, query: Query): string | undefined {
  const stepDefinition = query.findUnambiguousStepDefinitionBy(testStep)
  if (stepDefinition) {
    let result = stepDefinition.sourceReference.uri
    if (stepDefinition.sourceReference.location) {
      result += `:${stepDefinition.sourceReference.location.line}`
    }
    return result
  }
}

export function formatStepArgument(
  testStepFinished: TestStepFinished,
  query: Query
): string | undefined {
  return withStep(
    testStepFinished,
    query,
    ({ pickleStep }) => {
      if (pickleStep.argument?.docString) {
        return formatDocString(pickleStep.argument.docString)
      }
    },
    undefined
  )
}

function formatDocString(docString: PickleDocString) {
  return `"""${docString.mediaType ?? ''}
${docString.content}
"""`
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

function withScenario<T>(
  testCaseStarted: TestCaseStarted,
  query: Query,
  fn: (found: { pickle: Pickle; scenario: Scenario }) => T,
  fallback: T
): T {
  const pickle = query.findPickleBy(testCaseStarted)
  if (!pickle) {
    return fallback
  }
  const lineage = query.findLineageBy(pickle)
  if (!lineage) {
    return fallback
  }
  const scenario = lineage.scenario
  if (!scenario) {
    return fallback
  }
  return fn({
    pickle,
    scenario,
  })
}

function withStep<T>(
  testStepFinished: TestStepFinished,
  query: Query,
  fn: (found: { testStep: TestStep; pickleStep: PickleStep; step: Step }) => T,
  fallback: T
): T {
  const testStep = query.findTestStepBy(testStepFinished)
  if (!testStep) {
    return fallback
  }
  const pickleStep = query.findPickleStepBy(testStep)
  if (!pickleStep) {
    return fallback
  }
  const step = query.findStepBy(pickleStep)
  if (!step) {
    return fallback
  }
  return fn({
    testStep,
    pickleStep,
    step,
  })
}
