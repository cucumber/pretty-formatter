import { Envelope, TestCaseFinished, TestStepResultStatus } from '@cucumber/messages'
import { Query } from '@cucumber/query'

import {
  composeGlobalHookSummary,
  composeScenarioSummary,
  composeSnippets,
  composeStats,
} from './composition'
import {
  formatError,
  formatForStatus,
  formatStatusName,
  formatUndefinedParameterType,
} from './formatting'
import { findAllSuggestions, findAllTestCaseFinishedInCanonicalOrder } from './queries'
import { CUCUMBER_THEME } from './theme'
import type { SummaryOptions } from './types'
import {
  GHERKIN_INDENT_LENGTH,
  indent,
  indentNumbered,
  NON_REPORTABLE_STATUSES,
  ORDERED_STATUSES,
} from './utils'

const DEFAULT_OPTIONS: Required<SummaryOptions> = {
  includeAttachments: true,
  theme: CUCUMBER_THEME,
}

/**
 * Prints a summary of test results including non-passing scenarios, statistics, and snippets
 *
 * @remarks
 * Outputs non-passing scenarios and hooks, scenario and step counts, durations, and suggested
 * snippets for undefined steps.
 */
export class SummaryPrinter {
  private readonly stream: NodeJS.WritableStream
  private readonly print: (content: string) => void
  private readonly println: (content?: string) => void
  private readonly options: Required<SummaryOptions>
  private query: Query = new Query()

  /**
   * Creates a new SummaryPrinter instance
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
    options?: SummaryOptions
  } = {}) {
    this.stream = stream
    this.print = (content) => stream.write(content)
    this.println = (content = '') => this.print(`${content}\n`)
    this.options = {
      ...DEFAULT_OPTIONS,
      ...options,
    }
  }

  /**
   * Processes a Cucumber message envelope and prints if appropriate
   *
   * @param envelope - The Cucumber message envelope to process
   */
  public update(envelope: Envelope) {
    this.query.update(envelope)
    if (envelope.testRunFinished) {
      this.printSummary()
    }
  }

  /**
   * Immediately print a summary from pre-populated Query object
   *
   * @param query - A pre-populated Query object to be summarised
   * @param params - See constructor
   */
  public static summarise(
    query: Query,
    {
      stream = process.stdout,
      options = {},
    }: {
      stream?: NodeJS.WritableStream
      options?: SummaryOptions
    } = {}
  ) {
    const printer = new SummaryPrinter({
      stream,
      options,
    })
    printer.query = query
    printer.printSummary()
  }

  private printSummary() {
    this.printNonPassingScenarios()
    this.printUnknownParameterTypes()
    this.printNonPassingGlobalHooks()
    this.printNonPassingTestRun()
    this.printStats()
    this.printSnippets()
  }

  private printStats() {
    this.println()
    this.println(composeStats(this.query, this.options.theme, this.stream))
  }

  private printNonPassingScenarios() {
    const nonPassingScenarios = this.resolveNonPassingScenarios()

    for (const status of ORDERED_STATUSES) {
      const forThisStatus = nonPassingScenarios.get(status) ?? []
      if (forThisStatus.length > 0) {
        this.println()
        this.println(
          formatForStatus(
            status,
            `${formatStatusName(status)} scenarios:`,
            this.options.theme,
            this.stream
          )
        )
        forThisStatus.forEach((testCaseFinished, index) => {
          const formatted = composeScenarioSummary(
            testCaseFinished,
            this.query,
            this.options,
            this.stream
          )
          this.println(indentNumbered(formatted, 2, index + 1))
        })
      }
    }
  }

  private resolveNonPassingScenarios(): Map<TestStepResultStatus, TestCaseFinished[]> {
    const reportableByStatus = new Map<TestStepResultStatus, TestCaseFinished[]>()

    for (const testCaseFinished of findAllTestCaseFinishedInCanonicalOrder(this.query)) {
      const mostSevereResult = this.query.findMostSevereTestStepResultBy(testCaseFinished)

      if (mostSevereResult && !NON_REPORTABLE_STATUSES.includes(mostSevereResult.status)) {
        if (!reportableByStatus.has(mostSevereResult.status)) {
          reportableByStatus.set(mostSevereResult.status, [])
        }
        reportableByStatus.get(mostSevereResult.status)?.push(testCaseFinished)
      }
    }
    return reportableByStatus
  }

  private printUnknownParameterTypes() {
    const unknownParameterTypes = this.query.findAllUndefinedParameterTypes()
    if (unknownParameterTypes.length > 0) {
      this.println()
      this.println(
        formatForStatus(
          TestStepResultStatus.UNDEFINED,
          'These parameters are missing a parameter type definition:',
          this.options.theme,
          this.stream
        )
      )
      unknownParameterTypes.forEach((upt, index) => {
        this.println(
          indentNumbered(formatUndefinedParameterType(upt), GHERKIN_INDENT_LENGTH, index + 1)
        )
      })
    }
  }

  private printNonPassingGlobalHooks() {
    const testRunHookFinished = this.query.findAllTestRunHookFinished()
    const failedHooks = testRunHookFinished.filter(
      (hook) => hook.result.status === TestStepResultStatus.FAILED
    )

    if (failedHooks.length > 0) {
      this.println()
      this.println(
        formatForStatus(
          TestStepResultStatus.FAILED,
          `${formatStatusName(TestStepResultStatus.FAILED)} hooks:`,
          this.options.theme,
          this.stream
        )
      )
      failedHooks.forEach((testRunHookFinished, index) => {
        const formatted = composeGlobalHookSummary(
          testRunHookFinished,
          this.query,
          this.options,
          this.stream
        )
        this.println(indentNumbered(formatted, GHERKIN_INDENT_LENGTH, index + 1))
      })
    }
  }

  private printNonPassingTestRun() {
    const testRunFinished = this.query.findTestRunFinished()
    if (testRunFinished?.exception) {
      this.println(
        formatForStatus(
          TestStepResultStatus.FAILED,
          `${formatStatusName(TestStepResultStatus.FAILED)} test run:`,
          this.options.theme,
          this.stream
        )
      )
      const error = testRunFinished.exception?.stackTrace || testRunFinished.exception?.message
      if (error) {
        this.print(
          indent(
            formatError(error, TestStepResultStatus.FAILED, this.options.theme, this.stream),
            7
          )
        )
      }
    }
  }

  private printSnippets() {
    const suggestions = findAllSuggestions(this.query)
    if (suggestions.length > 0) {
      this.print(composeSnippets(suggestions))
    }
  }
}
