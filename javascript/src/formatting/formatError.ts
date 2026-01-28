import { TestStepResultStatus } from '@cucumber/messages'

import { TextBuilder } from '../TextBuilder'
import { Theme } from '../types'

export function formatError(
  message: string,
  status: TestStepResultStatus,
  theme: Theme,
  stream: NodeJS.WritableStream
): string {
  return new TextBuilder(stream).append(message.trim()).build(theme.status?.all?.[status], true)
}
