import { TestCaseFinished } from '@cucumber/messages'
import { Query } from '@cucumber/query'

// TODO https://github.com/cucumber/query/pull/114
export function findAllTestCaseFinishedInCanonicalOrder(
  query: Query
): ReadonlyArray<TestCaseFinished> {
  return query.findAllTestCaseFinished()
}
