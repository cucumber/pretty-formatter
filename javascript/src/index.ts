import { Envelope } from '@cucumber/messages'

import { PrettyPrinter } from './PrettyPrinter.js'
import { CUCUMBER_THEME } from './theme.js'
import type { Options } from './types.js'

const DEFAULT_OPTIONS: Required<Options> = {
  featuresAndRules: true,
  theme: CUCUMBER_THEME,
}

export * from './theme.js'
export type * from './types.js'

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
