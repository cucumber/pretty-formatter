import type { StepDefinition } from '@cucumber/messages'

import { TextBuilder } from '../TextBuilder.js'
import type { Theme } from '../types.js'
import { formatSourceReference } from './formatSourceReference.js'

export function formatAmbiguousStep(
  stepDefinitions: readonly StepDefinition[],
  theme: Theme,
  stream: NodeJS.WritableStream
): string {
  const builder = new TextBuilder(stream)
  builder.append('Multiple matching step definitions found:')
  for (const stepDefinition of stepDefinitions) {
    builder.line()
    builder.append(`  ${theme.symbol?.bullet || ' '} `)
    if (stepDefinition.pattern?.source) {
      builder.append(stepDefinition.pattern.source)
    }
    const location = formatSourceReference(stepDefinition.sourceReference, theme, stream)
    if (location) {
      builder.space().append(location)
    }
  }
  return builder.build(undefined, true)
}
