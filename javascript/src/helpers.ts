import {
  Pickle,
  PickleStep,
  Scenario,
  Step,
  TestCaseStarted,
  TestStep,
  TestStepFinished,
} from '@cucumber/messages'
import { Query } from '@cucumber/query'

const NAME_DELIMITER_LENGTH = 2
const STEP_INDENT_LENGTH = 2
const ERROR_INDENT_LENGTH = 4

export function preCalculateMaxContentLength(
  testCaseStarted: TestCaseStarted,
  query: Query
): number {
  return withScenario(
    testCaseStarted,
    query,
    ({ pickle, scenario }) => {
      const scenarioLength = scenario.keyword.length + NAME_DELIMITER_LENGTH + pickle.name.length
      const stepLengths = pickle.steps.map((pickleStep) => {
        const step = query.findStepBy(pickleStep)
        return STEP_INDENT_LENGTH + (step?.keyword.length ?? 0) + pickleStep.text.length
      })
      return Math.max(scenarioLength, ...stepLengths)
    },
    0
  )
}

export function formatPickleLocation(pickle: Pickle, query: Query): string {
  let result = pickle.uri
  const location = query.findLocationOf(pickle)
  if (location) {
    result += `:${location.line}`
  }
  return result
}

export function formatStepLocation(testStep: TestStep, query: Query): string | undefined {
  const stepDefinition = query.findUnambiguousStepDefinitionBy(testStep)
  if (stepDefinition) {
    let result = stepDefinition.sourceReference.uri
    if (stepDefinition.sourceReference.location) {
      result += `:${stepDefinition.sourceReference.location.line}`
    }
    return result
  }
}

export function formatError(content: string): string {
  return content
    .split('\n')
    .map((line) => `${' '.repeat(STEP_INDENT_LENGTH + ERROR_INDENT_LENGTH)}${line}`)
    .join('\n')
}

export function withScenario<T>(
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

export function withStep<T>(
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
