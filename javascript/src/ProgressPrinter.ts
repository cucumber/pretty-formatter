import { Envelope } from '@cucumber/messages'
import { Query } from '@cucumber/query'

import { formatStatusCharacter } from './helpers'
import { SummaryPrinter } from './SummaryPrinter'
import { CUCUMBER_THEME } from './theme'
import type { ProgressOptions } from './types'

const DEFAULT_OPTIONS: Required<ProgressOptions> = {
  theme: CUCUMBER_THEME,
}

export class ProgressPrinter {
  private readonly query: Query = new Query()
  private readonly options: Required<ProgressOptions>

  constructor(
    private readonly stream: NodeJS.WritableStream,
    private readonly print: (content: string) => void,
    options: ProgressOptions = {}
  ) {
    this.options = {
      ...DEFAULT_OPTIONS,
      ...options,
    }
  }

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
