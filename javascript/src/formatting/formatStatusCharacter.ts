import type { TestStepResultStatus } from '@cucumber/messages'

import { TextBuilder } from '../TextBuilder'
import type { Theme } from '../types'

export function formatStatusCharacter(
  status: TestStepResultStatus,
  theme: Theme,
  stream: NodeJS.WritableStream
): string {
  const character = theme.status?.progress?.[status] || ' '
  return new TextBuilder(stream).append(character).build(theme.status?.all?.[status])
}
