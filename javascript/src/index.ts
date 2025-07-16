import { Envelope } from '@cucumber/messages'

import { PrettyPrinter } from './PrettyPrinter.js'
import type { Options } from './types.js'

export * from './theme.js'
export type * from './types.js'

export default {
  type: 'formatter',
  formatter({
    on,
    options = {},
    write,
  }: {
    on: (type: 'message', handler: (message: Envelope) => void) => void
    options?: Options
    write: (content: string) => void
  }) {
    const printer = new PrettyPrinter(write, options)
    on('message', (message: Envelope) => printer.update(message))
  },
  optionsKey: 'pretty',
}
