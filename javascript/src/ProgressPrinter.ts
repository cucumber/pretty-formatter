import { Envelope } from '@cucumber/messages'
import { Query } from '@cucumber/query'

import { formatStatusCharacter } from './helpers'
import { SummaryPrinter } from './SummaryPrinter'
import type { ProgressOptions } from './types'

export class ProgressPrinter {
  private readonly query: Query = new Query()

  constructor(
    private readonly stream: NodeJS.WritableStream,
    private readonly print: (content: string) => void,
    private readonly options: Required<ProgressOptions>
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

  summarise() {
    new SummaryPrinter(this.query, this.stream, this.print, this.options).printSummary()
  }
}
