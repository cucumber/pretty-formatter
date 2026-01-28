import { Envelope } from '@cucumber/messages'
import { Query } from '@cucumber/query'

import { formatStatusCharacter } from './formatting'
import { SummaryPrinter } from './SummaryPrinter'
import { CUCUMBER_THEME } from './theme'
import type { ProgressOptions } from './types'

const DEFAULT_OPTIONS: Required<ProgressOptions> = {
  includeAttachments: true,
  summarise: false,
  theme: CUCUMBER_THEME,
}

/**
 * Prints test progress as single-character status indicators
 *
 * @remarks
 * Outputs a character for each test step as they complete, providing a compact
 * real-time view of test progress. Characters are styled according to their status
 * (e.g. `.` for passed, `F` for failed etc).
 */
export class ProgressPrinter {
  private readonly stream: NodeJS.WritableStream
  private readonly print: (content: string) => void
  private readonly options: Required<ProgressOptions>
  private readonly query: Query = new Query()

  /**
   * Creates a new ProgressPrinter instance
   *
   * @param params - Initialisation object
   * @param params.stream - The stream being written to, used for feature detection
   * @param params.options - Options for the output
   */
  constructor({
    stream = process.stdout,
    options = {},
  }: {
    stream?: NodeJS.WritableStream
    options?: ProgressOptions
  } = {}) {
    this.stream = stream
    this.print = (content) => stream.write(content)
    this.options = {
      ...DEFAULT_OPTIONS,
      ...options,
    }
  }

  /**
   * Processes a Cucumber message envelope and prints if appropriate
   *
   * @param message - The Cucumber message envelope to process
   */
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
      if (this.options.summarise) {
        this.summarise()
      }
    }
  }

  private summarise() {
    SummaryPrinter.summarise(this.query, {
      stream: this.stream,
      options: this.options,
    })
  }
}
