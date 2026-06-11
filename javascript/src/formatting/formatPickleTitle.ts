import type { Pickle, Scenario } from '@cucumber/messages'

import { TextBuilder } from '../TextBuilder.js'
import type { Theme } from '../types.js'

export function formatPickleTitle(
  pickle: Pickle,
  scenario: Scenario,
  theme: Theme,
  stream: NodeJS.WritableStream
): string {
  return new TextBuilder(stream)
    .append(`${scenario.keyword}:`, theme.scenario?.keyword)
    .space()
    .append(pickle.name || '', theme.scenario?.name)
    .build(theme.scenario?.all)
}
