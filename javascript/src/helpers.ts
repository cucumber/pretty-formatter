import {
  Hook,
  PickleStep,
  Step,
  StepDefinition,
  TestStep,
  TestStepFinished,
} from '@cucumber/messages'
import { Query } from '@cucumber/query'

import { ensure } from './utils'

type ResolvedPickleStep = {
  testStep: TestStep
  pickleStep: PickleStep
  step: Step
  stepDefinition?: StepDefinition
}

type ResolvedHookStep = {
  testStep: TestStep
  hook: Hook
}

type ResolvedStep = ResolvedPickleStep | ResolvedHookStep

export function resolveStep(
  testStepFinished: TestStepFinished,
  query: Query
): ResolvedStep | undefined {
  const testStep = ensure(
    query.findTestStepBy(testStepFinished),
    'TestStep must exist for TestStepFinished'
  )
  if (testStep.pickleStepId) {
    const pickleStep = query.findPickleStepBy(testStep)
    if (!pickleStep) {
      return undefined
    }
    const step = ensure(query.findStepBy(pickleStep), 'Step must exist for PickleStep')
    const stepDefinition = query.findUnambiguousStepDefinitionBy(testStep)
    return {
      testStep,
      pickleStep,
      step,
      stepDefinition,
    }
  } else if (testStep.hookId) {
    const hook = query.findHookBy(testStep)
    if (!hook) {
      return undefined
    }
    return {
      testStep,
      hook,
    }
  }
}
