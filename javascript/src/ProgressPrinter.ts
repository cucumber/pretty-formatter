import { Envelope, TestRunFinished } from '@cucumber/messages'
import { Query } from '@cucumber/query'

import type { Options } from './types.js'

export class ProgressPrinter {
  private readonly println: (content?: string) => void
  private readonly query: Query = new Query()

  constructor(
    private readonly stream: NodeJS.WritableStream,
    private readonly print: (content: string) => void,
    private readonly options: Required<Options>
  ) {
    this.println = (content: string = '') => this.print(`${content}\n`)
  }

  update(message: Envelope) {
    this.query.update(message)

    if (message.testRunFinished) {
      this.handleTestRunFinished(message.testRunFinished)
    }
  }

  private handleTestRunFinished(testRunFinished: TestRunFinished) {
    this.println('Progress!')
  }
}