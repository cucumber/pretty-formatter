import { WriteStream } from 'node:tty'

import {
  Envelope,
  TestCase,
  TestCaseFinished,
  TestRunFinished,
  TestRunHookFinished,
  TestStepResultStatus,
  UndefinedParameterType,
} from '@cucumber/messages'
import { Query } from '@cucumber/query'

import {
  composeGlobalHookSummary,
  composeScenarioSummary,
  composeSnippets,
  composeStats,
} from './composition'
import { defaultFormatCode } from './defaultFormatCode'
import { formatError, formatUndefinedParameterType } from './formatting'
import { formatProblem } from './formatting/formatProblem'
import { findAllSuggestions } from './queries'
import { CUCUMBER_THEME } from './theme'
import { ProgressBarOptions } from './types'
import { indent, indentNumbered, NON_REPORTABLE_STATUSES, ProblemType } from './utils'

enum Phase {
  PREPARING,
  RUNNING,
  DONE,
}

type Problem = {
  type: ProblemType
  details: string
}

const DEFAULT_OPTIONS: Required<ProgressBarOptions> = {
  includeAttachments: true,
  summarise: false,
  theme: CUCUMBER_THEME,
  formatCode: defaultFormatCode,
}
const MAX_BAR_WIDTH = 50
const MIN_LEGEND_WIDTH = 30

/**
 * Prints test progress as an updating progress bar, with problems prepended above
 *
 * @remarks
 * Requires a TTY terminal.
 */
export class ProgressBarPrinter {
  private readonly stream: WriteStream
  private readonly options: Required<ProgressBarOptions>
  private readonly query: Query = new Query()
  private readonly printedProblems: Array<Problem> = []
  private readonly pendingProblems: Array<Problem> = []
  private phase: Phase = Phase.PREPARING
  private finishedScenarios = 0
  private finishedSteps = 0
  private runningScenarios = 0
  private totalScenarios = 0
  private totalSteps = 0

  /**
   * Creates a new ProgressBarPrinter instance
   *
   * @param params - Initialisation object
   * @param params.stream - The stream being written to, used for feature detection
   * @param params.options - Options for the output
   */
  constructor({
    stream = process.stdout,
    options = {},
  }: {
    stream?: WriteStream
    options?: ProgressBarOptions
  } = {}) {
    this.stream = stream
    this.options = {
      ...DEFAULT_OPTIONS,
      ...options,
    }
    this.rerender(true)
  }

  /**
   * Processes a Cucumber message envelope and prints if appropriate
   *
   * @param message - The Cucumber message envelope to process
   */
  update(message: Envelope) {
    this.query.update(message)

    if (this.phase === Phase.DONE) {
      return
    }

    if (message.undefinedParameterType) {
      this.handleUndefinedParameterType(message.undefinedParameterType)
    }

    if (message.testRunStarted) {
      this.handleTestRunStarted()
    }

    if (message.testCase) {
      this.handleTestCase(message.testCase)
    }

    if (message.testRunHookFinished) {
      this.handleTestRunHookFinished(message.testRunHookFinished)
    }

    if (message.testCaseStarted) {
      this.handleTestCaseStarted()
    }

    if (message.testStepFinished) {
      this.handleTestStepFinished()
    }

    if (message.testCaseFinished) {
      this.handleTestCaseFinished(message.testCaseFinished)
    }

    if (message.testRunFinished) {
      this.handleTestRunFinished(message.testRunFinished)
    }
  }

  private handleUndefinedParameterType(undefinedParameterType: UndefinedParameterType) {
    this.pendingProblems.push({
      type: ProblemType.PARAMETER_TYPE,
      details: formatUndefinedParameterType(undefinedParameterType),
    })
    this.rerender()
  }

  private handleTestRunStarted() {
    this.phase = Phase.RUNNING
    this.rerender()
  }

  private handleTestCase(testCase: TestCase) {
    this.totalScenarios++
    this.totalSteps += testCase.testSteps.length
    this.rerender()
  }

  private handleTestRunHookFinished(testRunHookFinished: TestRunHookFinished) {
    if (!NON_REPORTABLE_STATUSES.includes(testRunHookFinished.result.status)) {
      this.pendingProblems.push({
        type: ProblemType.GLOBAL_HOOK,
        details: composeGlobalHookSummary(
          testRunHookFinished,
          this.query,
          this.options,
          this.stream
        ),
      })
      this.rerender()
    }
  }

  private handleTestCaseStarted() {
    this.runningScenarios++
  }

  private handleTestStepFinished() {
    this.finishedSteps++
    this.rerender()
  }

  private handleTestCaseFinished(testCaseFinished: TestCaseFinished) {
    this.runningScenarios--
    this.finishedScenarios++

    if (testCaseFinished.willBeRetried) {
      const testCase = this.query.findTestCaseBy(testCaseFinished)
      if (testCase) {
        this.finishedScenarios--
        this.finishedSteps = this.finishedSteps - testCase.testSteps.length
      }
    } else {
      const mostSevereResult = this.query.findMostSevereTestStepResultBy(testCaseFinished)
      if (mostSevereResult && !NON_REPORTABLE_STATUSES.includes(mostSevereResult.status)) {
        this.pendingProblems.push({
          type: ProblemType.TEST_CASE,
          details: composeScenarioSummary(testCaseFinished, this.query, this.options, this.stream),
        })
      }
    }
    this.rerender()
  }

  private handleTestRunFinished(testRunFinished: TestRunFinished) {
    this.phase = Phase.DONE
    if (testRunFinished.exception) {
      const error =
        testRunFinished.exception?.stackTrace ||
        testRunFinished.exception?.message ||
        'Unknown error'
      this.pendingProblems.push({
        type: ProblemType.TEST_RUN,
        details:
          '\n' +
          indent(
            formatError(error, TestStepResultStatus.FAILED, this.options.theme, this.stream),
            4
          ),
      })
    }
    this.rerender()
  }

  private rerender(initial = false) {
    let output = ''
    if (this.pendingProblems.length > 0) {
      if (this.printedProblems.length === 0) {
        output += 'Problems:\n'
      }
      const problems = this.pendingProblems.splice(0)
      for (const { type, details } of problems) {
        const prefixed = formatProblem(type, details, this.options.theme, this.stream)
        const number = this.printedProblems.length + 1
        output += `${indentNumbered(prefixed, 2, number)}\n`
        this.printedProblems.push({ type, details })
      }
    }
    if (this.phase === Phase.DONE && this.options.summarise) {
      output += this.makeSummaryBlock()
    } else {
      output += this.makeProgressBlock()
    }
    this.render(output, initial)
  }

  private makeSummaryBlock() {
    let output = '\n'
    output += composeStats(this.query, this.options.theme, this.stream)
    output += '\n'
    const suggestions = findAllSuggestions(this.query)
    if (suggestions.length > 0) {
      output += composeSnippets(suggestions, this.options.formatCode, this.stream)
      output += '\n'
    }
    return output
  }

  private makeProgressBlock() {
    return [
      '',
      this.makeBar(this.finishedScenarios, this.totalScenarios, 'scenarios'),
      this.makeBar(this.finishedSteps, this.totalSteps, 'steps'),
      this.makeStatus(),
      '',
    ].join('\n')
  }

  private makeBar(finished: number, total: number, label: string) {
    const barWidth = Math.min(this.stream.columns - MIN_LEGEND_WIDTH, MAX_BAR_WIDTH)
    const ratio = total > 0 ? finished / total : 0
    const filledCount = Math.round(ratio * barWidth)
    const emptyCount = barWidth - filledCount
    const filled = '█'.repeat(filledCount)
    const empty = '░'.repeat(emptyCount)
    return `${filled}${empty} ${finished}/${total} ${label}`
  }

  private makeStatus() {
    switch (this.phase) {
      case Phase.PREPARING:
        return 'Getting ready...'
      case Phase.RUNNING:
        return `Running ${this.runningScenarios} scenarios...`
      case Phase.DONE:
        return 'Done'
    }
  }

  private render(content: string, initial: boolean) {
    if (!initial) {
      this.stream.moveCursor(0, -4)
      this.stream.clearScreenDown()
    }
    this.stream.write(content)
  }
}
