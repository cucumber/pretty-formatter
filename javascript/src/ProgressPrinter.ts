import { Envelope } from '@cucumber/messages'
import { Query } from '@cucumber/query'

import { formatStatusCharacter } from './helpers'
import type { Options } from './types'

export class ProgressPrinter {
  private readonly query: Query = new Query()

  constructor(
    private readonly stream: NodeJS.WritableStream,
    private readonly print: (content: string) => void,
    private readonly options: Required<Options>
  ) {}

  update(message: Envelope) {
    this.query.update(message)

    if (message.testStepFinished) {
      this.print(
        formatStatusCharacter(
          message.testStepFinished.testStepResult.status,
          this.options.theme,
          this.stream
        )
      )
    }

    if (message.testRunHookFinished) {
      this.print(
        formatStatusCharacter(
          message.testRunHookFinished.result.status,
          this.options.theme,
          this.stream
        )
      )
    }

    if (message.testRunFinished) {
      this.print('\n')
    }
  }
}
