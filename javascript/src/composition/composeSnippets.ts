import { Suggestion } from '@cucumber/messages'

export function composeSnippets(suggestions: ReadonlyArray<Suggestion>): string {
  const snippets = suggestions
    .flatMap((suggestion) => suggestion.snippets)
    .map((snippet) => snippet.code)

  const uniqueSnippets = new Set(snippets)

  const lines: Array<string> = []
  lines.push('')
  lines.push('You can implement missing steps with the snippets below:')
  lines.push('')
  for (const snippet of uniqueSnippets) {
    lines.push(snippet)
    lines.push('')
  }

  return lines.join('\n')
}
