import type { SourceReference } from '@cucumber/messages'

import { TextBuilder } from '../TextBuilder'
import type { Theme } from '../types'

export function formatSourceReference(
  sourceReference: SourceReference,
  theme: Theme,
  stream: NodeJS.WritableStream
): string {
  const builder = new TextBuilder(stream)
    .append('#')
    .space()
    .append(sourceReference.uri ?? '(unknown)')
  if (sourceReference.location) {
    builder.append(':').append(sourceReference.location.line)
  }
  return builder.build(theme.location)
}
