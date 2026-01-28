import { Pickle, Scenario } from '@cucumber/messages'

import { TextBuilder } from '../TextBuilder'
import { Theme } from '../types'

export function formatPickleTitle(
  pickle: Pickle,
  scenario: Scenario,
  theme: Theme,
  stream: NodeJS.WritableStream
): string {
  return new TextBuilder(stream)
    .append(scenario.keyword + ':', theme.scenario?.keyword)
    .space()
    .append(pickle.name || '', theme.scenario?.name)
    .build(theme.scenario?.all)
}
