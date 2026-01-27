import { StepDefinition } from '@cucumber/messages'

import { TextBuilder } from '../TextBuilder'
import { Theme } from '../types'
import { formatSourceReference } from './formatSourceReference'

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
