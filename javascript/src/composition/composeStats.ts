import { TestStepResultStatus } from '@cucumber/messages'
import { Query } from '@cucumber/query'

import { formatCounts, formatDurations } from '../formatting'
import type { Theme } from '../types'

export function composeStats(query: Query, theme: Theme, stream: NodeJS.WritableStream): string {
  const lines: Array<string> = []

  const testRunFinished = query.findTestRunFinished()
  if (testRunFinished?.exception) {
    lines.push('')
    lines.push(
      formatCounts(
        'test run',
        {
          [TestStepResultStatus.FAILED]: 1,
        },
        theme,
        stream
      )
    )
  }

  const testRunHookFinished = query.findAllTestRunHookFinished()
  if (testRunHookFinished.length > 0) {
    const globalHookCountsByStatus = testRunHookFinished
      .map((hook) => hook.result.status)
      .reduce(
        (prev, status) => {
          return {
            ...prev,
            [status]: (prev[status] ?? 0) + 1,
          }
        },
        {} as Partial<Record<TestStepResultStatus, number>>
      )
    lines.push(formatCounts('hooks', globalHookCountsByStatus, theme, stream))
  }

  const scenarioCountsByStatus = query
    .findAllTestCaseFinished()
    .map((testCaseFinished) => query.findMostSevereTestStepResultBy(testCaseFinished))
    .map((testStepResult) => testStepResult?.status ?? TestStepResultStatus.PASSED)
    .reduce(
      (prev, status) => {
        return {
          ...prev,
          [status]: (prev[status] ?? 0) + 1,
        }
      },
      {} as Partial<Record<TestStepResultStatus, number>>
    )
  lines.push(formatCounts('scenarios', scenarioCountsByStatus, theme, stream))

  const stepCountsByStatus = query
    .findAllTestCaseFinished()
    .flatMap((testCaseFinished) => query.findTestStepsFinishedBy(testCaseFinished))
    .map((testStepFinished) => testStepFinished.testStepResult.status)
    .reduce(
      (prev, status) => {
        return {
          ...prev,
          [status]: (prev[status] ?? 0) + 1,
        }
      },
      {} as Partial<Record<TestStepResultStatus, number>>
    )
  lines.push(formatCounts('steps', stepCountsByStatus, theme, stream))

  const testRunDuration = query.findTestRunDuration()
  if (testRunDuration) {
    const testRunHookDurations = query
      .findAllTestRunHookFinished()
      .map((hookFinished) => hookFinished.result.duration)
    const testStepDurations = query
      .findAllTestStepFinished()
      .map((stepFinished) => stepFinished.testStepResult.duration)
    const executionDurations = [...testRunHookDurations, ...testStepDurations]
    lines.push(formatDurations(testRunDuration, executionDurations))
  }

  return lines.join('\n')
}
