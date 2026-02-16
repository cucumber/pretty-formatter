import { stripVTControlCharacters } from 'node:util'

import { TestStepResult, TestStepResultStatus } from '@cucumber/messages'

export const GHERKIN_INDENT_LENGTH = 2
export const STEP_ARGUMENT_INDENT_LENGTH = 2
export const ATTACHMENT_INDENT_LENGTH = 4
export const ERROR_INDENT_LENGTH = 4
export const ORDERED_STATUSES: TestStepResultStatus[] = [
  TestStepResultStatus.UNKNOWN,
  TestStepResultStatus.PASSED,
  TestStepResultStatus.SKIPPED,
  TestStepResultStatus.PENDING,
  TestStepResultStatus.UNDEFINED,
  TestStepResultStatus.AMBIGUOUS,
  TestStepResultStatus.FAILED,
]
export const NON_REPORTABLE_STATUSES = [TestStepResultStatus.PASSED, TestStepResultStatus.SKIPPED]
export enum ProblemType {
  PARAMETER_TYPE,
  GLOBAL_HOOK,
  TEST_CASE,
  TEST_RUN,
}

export function ensure<T>(value: T | undefined, message: string): T {
  if (!value) {
    throw new Error(message)
  }
  return value
}

export function join(...originals: ReadonlyArray<string | undefined>) {
  return originals.filter((part) => !!part).join(' ')
}

export function indent(original: string, by: number) {
  return original
    .split('\n')
    .map((line) => ' '.repeat(by) + line)
    .join('\n')
}

export function indentNumbered(original: string, by: number, number: number): string {
  const baselineIndent = ' '.repeat(by)
  return original
    .split('\n')
    .map((line, i) => {
      if (line === '') {
        return ''
      }
      return i === 0 ? `${baselineIndent}${number}) ${line}` : `${baselineIndent}   ${line}`
    })
    .join('\n')
}

export function pad(original: string) {
  return `\n` + original + '\n'
}

export function unstyled(text: string) {
  return stripVTControlCharacters(text)
}

export function extractReportableMessage(testStepResult: TestStepResult): string | undefined {
  const { status, exception, message: standaloneMessage } = testStepResult

  if (status === TestStepResultStatus.FAILED) {
    return exception?.stackTrace || exception?.message || standaloneMessage
  }

  if (status === TestStepResultStatus.PENDING || status === TestStepResultStatus.SKIPPED) {
    return exception?.message || standaloneMessage
  }

  return undefined
}
