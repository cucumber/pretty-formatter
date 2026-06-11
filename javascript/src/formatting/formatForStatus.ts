import type { TestStepResultStatus } from '@cucumber/messages'

import { TextBuilder } from '../TextBuilder.js'
import type { Theme } from '../types.js'

export function formatForStatus(
  status: TestStepResultStatus,
  text: string,
  theme: Theme,
  stream: NodeJS.WritableStream
): string {
  return new TextBuilder(stream).append(text).build(theme.status?.all?.[status])
}
