import { Suggestion } from '@cucumber/messages'
import { Query } from '@cucumber/query'

export function findAllSuggestions(query: Query): ReadonlyArray<Suggestion> {
  return query
    .findAllTestCaseFinished()
    .map((testCaseFinished) => query.findPickleBy(testCaseFinished))
    .filter((pickle) => !!pickle)
    .flatMap((pickle) => query.findSuggestionsBy(pickle))
}
