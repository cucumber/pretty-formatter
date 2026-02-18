import { Suggestion } from '@cucumber/messages'

import { FormatCodeFunction } from '../types'

export function composeSnippets(
  suggestions: ReadonlyArray<Suggestion>,
  formatCode: FormatCodeFunction,
  stream: NodeJS.WritableStream
): string {
  const snippets = suggestions.flatMap((suggestion) => suggestion.snippets)

  // dedup by code while preserving full Snippet
  const seen = new Set<string>()
  const uniqueSnippets = snippets.filter((snippet) => {
    if (seen.has(snippet.code)) {
      return false
    }
    seen.add(snippet.code)
    return true
  })

  const lines: Array<string> = []
  lines.push('')
  lines.push('You can implement missing steps with the snippets below:')
  lines.push('')
  for (const snippet of uniqueSnippets) {
    lines.push(formatCode(snippet, stream))
    lines.push('')
  }

  return lines.join('\n')
}
