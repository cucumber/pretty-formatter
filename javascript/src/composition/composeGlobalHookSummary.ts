import { TestRunHookFinished, TestStepResultStatus } from '@cucumber/messages'
import { Query } from '@cucumber/query'

import { formatError, formatHookTitle, formatSourceReference } from '../formatting'
import type { SummaryOptions } from '../types'
import { GHERKIN_INDENT_LENGTH, indent, join } from '../utils'

export function composeGlobalHookSummary(
  testRunHookFinished: TestRunHookFinished,
  query: Query,
  { theme }: Required<SummaryOptions>,
  stream: NodeJS.WritableStream
): string {
  const hook = query.findHookBy(testRunHookFinished)
  const status = testRunHookFinished.result.status

  const lines: string[] = []

  lines.push(
    join(
      formatHookTitle(hook, status, {}, stream),
      hook?.sourceReference ? formatSourceReference(hook.sourceReference, theme, stream) : undefined
    )
  )

  if (status === TestStepResultStatus.FAILED) {
    const error =
      testRunHookFinished.result.exception?.stackTrace ||
      testRunHookFinished.result.exception?.message ||
      testRunHookFinished.result.message
    if (error) {
      lines.push(indent(formatError(error, status, theme, stream), GHERKIN_INDENT_LENGTH))
    }
  }

  return lines.join('\n')
}
