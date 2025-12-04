import { Envelope } from '@cucumber/messages'

import { PrettyPrinter } from './PrettyPrinter'
import { CUCUMBER_THEME } from './theme'
import type { Options } from './types'

const DEFAULT_OPTIONS: Required<Options> = {
  includeAttachments: true,
  includeFeatureLine: true,
  includeRuleLine: true,
  useStatusIcon: true,
  theme: CUCUMBER_THEME,
}

export * from './PrettyPrinter'
export * from './ProgressPrinter'
export * from './SummaryPrinter'
export * from './theme'
export type * from './types'

export default {
  type: 'formatter',
  formatter({
    on,
    options = {},
    stream = process.stdout,
    write,
  }: {
    on: (type: 'message', handler: (message: Envelope) => void) => void
    options?: Options
    stream?: NodeJS.WritableStream
    write: (content: string) => void
  }) {
    const printer = new PrettyPrinter(stream, write, {
      ...DEFAULT_OPTIONS,
      ...options,
    })
    on('message', (message: Envelope) => printer.update(message))
  },
  optionsKey: 'pretty',
}
