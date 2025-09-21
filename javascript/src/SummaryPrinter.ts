import {
  Envelope,
  Location,
  Pickle,
  TestCaseStarted,
  TestRunFinished,
  TestRunStarted,
  TestStepResult,
  TestStepResultStatus,
} from '@cucumber/messages'
import { Query } from '@cucumber/query'

import {
  ensure,
  ERROR_INDENT_LENGTH,
  formatCounts,
  formatDuration,
  formatNonPassingTitle,
  formatPickleLocation,
  formatTestStepResultError,
  GHERKIN_INDENT_LENGTH,
  indent,
  ORDERED_STATUSES,
} from './helpers.js'
import type { Options } from './types.js'

export class SummaryPrinter {
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
      this.printSummary()
    }
  }

  private printSummary() {
    this.printNonPassingScenarios()
    this.printStats()
    this.printSnippets()
  }

  private printStats() {
    this.println()
    this.printGlobalHookCounts()
    this.printScenarioCounts()
    this.printStepCounts()
    this.printDuration()
  }

  private printNonPassingScenarios() {
    const nonReportableStatuses = [TestStepResultStatus.PASSED, TestStepResultStatus.SKIPPED]
    const reportableByStatus = new Map<
      TestStepResultStatus,
      Array<{
        pickle: Pickle
        location: Location | undefined
        testCaseStarted: TestCaseStarted
        testStepResult: TestStepResult
      }>
    >()

    for (const testCaseFinished of this.query.findAllTestCaseFinished()) {
      // TODO canonical order
      const testCaseStarted = ensure(
        this.query.findTestCaseStartedBy(testCaseFinished),
        'TestCaseStarted must exist for TestCaseFinished'
      )
      const pickle = ensure(
        this.query.findPickleBy(testCaseFinished),
        'Pickle must exist for TestCaseFinished'
      )
      const location = this.query.findLocationOf(pickle)
      const testStepResult = this.query.findMostSevereTestStepResultBy(testCaseFinished)

      if (testStepResult && !nonReportableStatuses.includes(testStepResult.status)) {
        if (!reportableByStatus.has(testStepResult.status)) {
          reportableByStatus.set(testStepResult.status, [])
        }
        reportableByStatus.get(testStepResult.status)?.push({
          pickle,
          location,
          testCaseStarted,
          testStepResult,
        })
      }
    }

    for (const status of ORDERED_STATUSES) {
      const forThisStatus = reportableByStatus.get(status) ?? []
      if (forThisStatus.length > 0) {
        this.println()
        this.println(formatNonPassingTitle(status, this.options.theme, this.stream))
        forThisStatus.forEach(({ pickle, location, testCaseStarted, testStepResult }, index) => {
          const formattedLocation = formatPickleLocation(
            pickle,
            location,
            this.options.theme,
            this.stream
          )
          const formattedAttempt =
            testCaseStarted.attempt > 0 ? `, after ${testCaseStarted.attempt + 1} attempts` : ''
          this.println(
            indent(
              `${index + 1}) ${pickle.name}${formattedAttempt} ${formattedLocation}`,
              GHERKIN_INDENT_LENGTH
            )
          )
          if (status === TestStepResultStatus.FAILED) {
            const content = formatTestStepResultError(
              testStepResult,
              this.options.theme,
              this.stream
            )
            if (content) {
              this.println(indent(content, GHERKIN_INDENT_LENGTH + ERROR_INDENT_LENGTH + 1))
              this.println()
            }
          }
        })
      }
    }
  }

  private printGlobalHookCounts() {
    const testRunHookFinished = this.query.findAllTestRunHookFinished()
    if (testRunHookFinished.length === 0) {
      return
    }

    const globalHookCountsByStatus = testRunHookFinished
      .map((testRunHookFinished) => testRunHookFinished.result.status)
      .reduce(
        (prev, status) => {
          return {
            ...prev,
            [status]: (prev[status] ?? 0) + 1,
          }
        },
        {} as Partial<Record<TestStepResultStatus, number>>
      )
    this.println(formatCounts('hooks', globalHookCountsByStatus, this.options.theme, this.stream))
  }

  private printScenarioCounts() {
    const scenarioCountsByStatus = this.query
      .findAllTestCaseFinished()
      .map((testCaseFinished) => this.query.findMostSevereTestStepResultBy(testCaseFinished))
      .map((testStepResult) => testStepResult?.status ?? TestStepResultStatus.PASSED)
      .reduce(
        (prev, status) => {
          return {
            ...prev,
            [status]: (prev[status] ?? 0) + 1,
          }
        },
        {} as Partial<Record<TestStepResultStatus, number>>
      )
    this.println(formatCounts('scenarios', scenarioCountsByStatus, this.options.theme, this.stream))
  }

  private printStepCounts() {
    const stepCountsByStatus = this.query
      .findAllTestCaseFinished()
      .flatMap((testCaseFinished) => this.query.findTestStepsFinishedBy(testCaseFinished))
      .map((testStepFinished) => testStepFinished.testStepResult.status)
      .reduce(
        (prev, status) => {
          return {
            ...prev,
            [status]: (prev[status] ?? 0) + 1,
          }
        },
        {} as Partial<Record<TestStepResultStatus, number>>
      )
    this.println(formatCounts('steps', stepCountsByStatus, this.options.theme, this.stream))
  }

  private printDuration() {
    const testRunStarted = this.query.findTestRunStarted() as TestRunStarted
    const testRunFinished = this.query.findTestRunFinished() as TestRunFinished

    this.println(formatDuration(testRunStarted.timestamp, testRunFinished.timestamp))
  }

  private printSnippets() {
    const snippets = this.query
      .findAllTestCaseFinished() // TODO canonical order
      .map((testCaseFinished) => this.query.findPickleBy(testCaseFinished))
      .filter((pickle) => !!pickle)
      .flatMap((pickle) => this.query.findSuggestionsBy(pickle))
      .flatMap((suggestion) => suggestion.snippets)
      .map((snippet) => snippet.code)
    const uniqueSnippets = new Set(snippets)
    if (uniqueSnippets.size > 0) {
      this.println()
      this.println('You can implement missing steps with the snippets below:')
      this.println()
      for (const snippet of uniqueSnippets) {
        this.println(snippet)
        this.println()
      }
    }
  }
}
