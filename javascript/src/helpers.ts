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

export function preCalculateMaxContentLength(
  testCaseStarted: TestCaseStarted,
  query: Query
): number {
  return withScenario(
    testCaseStarted,
    query,
    ({ pickle, scenario }) => {
      const scenarioLength = scenario.keyword.length + pickle.name.length + 2
      const stepLengths = pickle.steps.map((pickleStep) => {
        const step = query.findStepBy(pickleStep)
        return (step?.keyword.length ?? 0) + pickleStep.text.length + 2
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
