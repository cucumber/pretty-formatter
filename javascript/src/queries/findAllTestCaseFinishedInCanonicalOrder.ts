import type { TestCaseFinished } from '@cucumber/messages'
import type { Query } from '@cucumber/query'

// TODO https://github.com/cucumber/query/pull/114
export function findAllTestCaseFinishedInCanonicalOrder(
  query: Query
): ReadonlyArray<TestCaseFinished> {
  return query.findAllTestCaseFinished()
}
