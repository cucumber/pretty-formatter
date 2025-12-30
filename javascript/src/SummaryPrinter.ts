import {
  Duration,
  Location,
  Pickle,
  TestCaseStarted,
  TestStep,
  TestStepFinished,
  TestStepResultStatus,
} from '@cucumber/messages'
import { Query } from '@cucumber/query'

import {
  ensure,
  ERROR_INDENT_LENGTH,
  formatCodeLocation,
  formatCounts,
  formatDurations,
  formatForStatus,
  formatHookTitle,
  formatPickleLocation,
  formatPickleStepArgument,
  formatStepTitle,
  formatTestRunFinishedError,
  formatTestStepResultError,
  GHERKIN_INDENT_LENGTH,
  indent,
  join,
  ORDERED_STATUSES,
  titleCaseStatus,
} from './helpers'
import { CUCUMBER_THEME } from './theme'
import type { SummaryOptions } from './types'

const DEFAULT_OPTIONS: Required<SummaryOptions> = {
  theme: CUCUMBER_THEME,
}

export class SummaryPrinter {
  private readonly println: (content?: string) => void
  private readonly options: Required<SummaryOptions>

  constructor(
    private readonly query: Query,
    private readonly stream: NodeJS.WritableStream,
    private readonly print: (content: string) => void,
    options: SummaryOptions = {}
  ) {
    this.println = (content: string = '') => this.print(`${content}\n`)
    this.options = {
      ...DEFAULT_OPTIONS,
      ...options,
    }
  }

  public printSummary() {
    this.printNonPassingScenarios()
    this.printUnknownParameterTypes()
    this.printNonPassingGlobalHooks()
    this.printNonPassingTestRun()
    this.printStats()
    this.printSnippets()
  }

  private printStats() {
    this.println()
    this.printTestRunCount()
    this.printGlobalHookCounts()
    this.printScenarioCounts()
    this.printStepCounts()
    this.printDurations()
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
            `${titleCaseStatus(status)} scenarios:`,
            this.options.theme,
            this.stream
          )
        )
        forThisStatus.forEach(
          (
            { pickle, location, testCaseStarted, responsibleStep: [testStepFinished, testStep] },
            index
          ) => {
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

            this.printResponsibleStep(testStepFinished, testStep, status)
          }
        )
      }
    }
  }

  private resolveNonPassingScenarios() {
    const nonReportableStatuses = [TestStepResultStatus.PASSED, TestStepResultStatus.SKIPPED]
    const reportableByStatus = new Map<
      TestStepResultStatus,
      Array<{
        pickle: Pickle
        location: Location | undefined
        testCaseStarted: TestCaseStarted
        responsibleStep: [TestStepFinished, TestStep]
      }>
    >()

    for (const testCaseFinished of this.findAllTestCaseFinishedInCanonicalOrder()) {
      const testCaseStarted = ensure(
        this.query.findTestCaseStartedBy(testCaseFinished),
        'TestCaseStarted must exist for TestCaseFinished'
      )
      const pickle = ensure(
        this.query.findPickleBy(testCaseFinished),
        'Pickle must exist for TestCaseFinished'
      )
      const location = this.query.findLocationOf(pickle)
      const mostSevereResult = this.query.findMostSevereTestStepResultBy(testCaseFinished)

      if (mostSevereResult && !nonReportableStatuses.includes(mostSevereResult.status)) {
        const responsibleStep = this.query
          .findTestStepFinishedAndTestStepBy(testCaseStarted)
          .find(
            ([testStepFinished]) =>
              testStepFinished.testStepResult.status === mostSevereResult.status
          ) as [TestStepFinished, TestStep]

        if (!reportableByStatus.has(mostSevereResult.status)) {
          reportableByStatus.set(mostSevereResult.status, [])
        }
        reportableByStatus.get(mostSevereResult.status)?.push({
          pickle,
          location,
          testCaseStarted,
          responsibleStep,
        })
      }
    }
    return reportableByStatus
  }

  private printResponsibleStep(
    testStepFinished: TestStepFinished,
    testStep: TestStep,
    status: TestStepResultStatus
  ) {
    if (testStep.pickleStepId) {
      const pickleStep = ensure(
        this.query.findPickleStepBy(testStep),
        'PickleStep must exist for Step with pickleStepId'
      )
      const step = ensure(this.query.findStepBy(pickleStep), 'Step must exist for PickleStep')
      this.println(
        indent(
          join(
            formatStepTitle(
              testStep,
              pickleStep,
              step,
              status,
              false,
              this.options.theme,
              this.stream
            ),
            formatCodeLocation(
              this.query.findUnambiguousStepDefinitionBy(testStep),
              this.options.theme,
              this.stream
            )
          ),
          7
        )
      )
      const argument = formatPickleStepArgument(pickleStep, this.options.theme, this.stream)
      if (argument) {
        this.println(indent(argument, 9))
      }
    } else if (testStep.hookId) {
      const hook = this.query.findHookBy(testStep)
      this.println(
        indent(
          join(
            formatHookTitle(hook, status, this.options.theme, this.stream),
            formatCodeLocation(hook, this.options.theme, this.stream)
          ),
          7
        )
      )
    }

    if (status === TestStepResultStatus.FAILED) {
      const content = formatTestStepResultError(
        testStepFinished.testStepResult,
        this.options.theme,
        this.stream
      )
      if (content) {
        this.println(indent(content, 11))
        this.println()
      }
    }
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
          indent(`${index + 1}) '${upt.name}' in '${upt.expression}'`, GHERKIN_INDENT_LENGTH)
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
          `${titleCaseStatus(TestStepResultStatus.FAILED)} hooks:`,
          this.options.theme,
          this.stream
        )
      )
      failedHooks.forEach((testRunHookFinished, index) => {
        const hook = this.query.findHookBy(testRunHookFinished)
        const formattedLocation = formatCodeLocation(hook, this.options.theme, this.stream)
        this.println(
          indent(
            `${index + 1}) ${formatHookTitle(hook, TestStepResultStatus.FAILED, {}, this.stream)} ${formattedLocation}`,
            GHERKIN_INDENT_LENGTH
          )
        )
        const content = formatTestStepResultError(
          testRunHookFinished.result,
          this.options.theme,
          this.stream
        )
        if (content) {
          this.println(indent(content, GHERKIN_INDENT_LENGTH + ERROR_INDENT_LENGTH + 1))
          this.println()
        }
      })
    }
  }

  private printNonPassingTestRun() {
    const testRunFinished = this.query.findTestRunFinished()
    if (testRunFinished?.exception) {
      this.println(
        formatForStatus(
          TestStepResultStatus.FAILED,
          `${titleCaseStatus(TestStepResultStatus.FAILED)} test run:`,
          this.options.theme,
          this.stream
        )
      )
      const formattedError = formatTestRunFinishedError(
        testRunFinished,
        this.options.theme,
        this.stream
      )
      if (formattedError) {
        this.println(indent(formattedError, 7))
      }
    }
  }

  private printTestRunCount() {
    const testRunFinished = this.query.findTestRunFinished()
    if (testRunFinished?.exception) {
      this.println()
      this.println(
        formatCounts(
          'test run',
          {
            [TestStepResultStatus.FAILED]: 1,
          },
          this.options.theme,
          this.stream
        )
      )
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

  private printDurations() {
    const testRunDuration = this.query.findTestRunDuration() as Duration

    const testRunHookDurations = this.query
      .findAllTestRunHookFinished()
      .map((hookFinished) => hookFinished.result.duration)
    const testStepDurations = this.query
      .findAllTestStepFinished()
      .map((stepFinished) => stepFinished.testStepResult.duration)
    const executionDurations = [...testRunHookDurations, ...testStepDurations]

    this.println(formatDurations(testRunDuration, executionDurations))
  }

  private printSnippets() {
    const snippets = this.findAllTestCaseFinishedInCanonicalOrder()
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

  private findAllTestCaseFinishedInCanonicalOrder() {
    // TODO https://github.com/cucumber/query/pull/114
    return this.query.findAllTestCaseFinished()
  }
}
