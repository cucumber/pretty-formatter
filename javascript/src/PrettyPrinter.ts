import {
  Attachment,
  Envelope,
  Feature,
  Location,
  Pickle,
  PickleStep,
  Rule,
  Scenario,
  Step,
  StepDefinition,
  TestCaseStarted,
  TestStep,
  TestStepFinished,
  TestStepResultStatus,
} from '@cucumber/messages'
import { Query } from '@cucumber/query'

import {
  ATTACHMENT_INDENT_LENGTH,
  ensure,
  ERROR_INDENT_LENGTH,
  formatAttachment,
  formatError,
  formatFeatureTitle,
  formatPickleLocation,
  formatPickleTags,
  formatPickleTitle,
  formatRuleTitle,
  formatStepArgument,
  formatStepLocation,
  formatStepTitle,
  GHERKIN_INDENT_LENGTH,
  indent,
  pad,
  STEP_ARGUMENT_INDENT_LENGTH,
  unstyled,
} from './helpers.js'
import { CUCUMBER_THEME } from './theme.js'
import type { Options } from './types.js'

export class PrettyPrinter {
  private readonly query: Query = new Query()
  private readonly scenarioIndentByTestCaseStartedId: Map<string, number> = new Map<
    string,
    number
  >()
  private readonly maxContentLengthByTestCaseStartedId: Map<string, number> = new Map<
    string,
    number
  >()
  private readonly encounteredFeaturesAndRules: Set<Feature | Rule> = new Set()
  private readonly println: (content?: string) => void
  private readonly options: Required<Options>

  constructor(
    private readonly stream: NodeJS.WritableStream,
    private readonly print: (content: string) => void,
    options: Options
  ) {
    this.println = (content: string = '') => this.print(`${content}\n`)
    this.options = {
      includeFeaturesAndRules: true,
      statusIcons: true,
      theme: CUCUMBER_THEME,
      ...options,
    }
  }

  update(message: Envelope) {
    this.query.update(message)

    if (message.testCaseStarted) {
      this.preCalculateIndentAndMaxContentLength(message.testCaseStarted)
      this.handleTestCaseStarted(message.testCaseStarted)
    }

    if (message.attachment) {
      this.handleAttachment(message.attachment)
    }

    if (message.testStepFinished) {
      this.handleTestStepFinished(message.testStepFinished)
    }
  }

  private resolveScenario(testCaseStarted: TestCaseStarted) {
    const pickle = ensure(
      this.query.findPickleBy(testCaseStarted),
      'Pickle must exist for TestCaseStarted'
    )
    const location = this.query.findLocationOf(pickle)
    const lineage = ensure(this.query.findLineageBy(pickle), 'Lineage must exist for Pickle')
    const scenario = ensure(lineage.scenario, 'Scenario must exist for Lineage')
    const rule = lineage.rule
    const feature = ensure(lineage.feature, 'Feature must exist for Lineage')
    return {
      pickle,
      location,
      scenario,
      rule,
      feature,
    }
  }

  private resolveStep(testStepFinished: TestStepFinished) {
    const testStep = ensure(
      this.query.findTestStepBy(testStepFinished),
      'TestStep must exist for TestStepFinished'
    )
    const pickleStep = this.query.findPickleStepBy(testStep)
    if (!pickleStep) {
      return undefined
    }
    const step = ensure(this.query.findStepBy(pickleStep), 'Step must exist for PickleStep')
    const stepDefinition = this.query.findUnambiguousStepDefinitionBy(testStep)
    return {
      testStep,
      pickleStep,
      step,
      stepDefinition,
    }
  }

  private preCalculateIndentAndMaxContentLength(testCaseStarted: TestCaseStarted) {
    const pickle = ensure(
      this.query.findPickleBy(testCaseStarted),
      'Pickle must exist for TestCaseStarted'
    )
    const lineage = ensure(this.query.findLineageBy(pickle), 'Lineage must exist for Pickle')
    const scenario = ensure(lineage.scenario, 'Scenario must exist for Lineage')
    const scenarioLength = unstyled(
      formatPickleTitle(pickle, scenario, this.options.theme, this.stream)
    ).length
    const testCase = ensure(
      this.query.findTestCaseBy(testCaseStarted),
      'TestCase must exist for TestCaseStarted'
    )
    const stepLengths = testCase.testSteps
      .filter((testStep) => !!testStep.pickleStepId)
      .map((testStep) => {
        const pickleStep = ensure(
          this.query.findPickleStepBy(testStep),
          'PickleStep must exist for TestStep'
        )
        const step = ensure(this.query.findStepBy(pickleStep), 'Step must exist for PickleStep')
        return indent(
          unstyled(
            formatStepTitle(
              testStep,
              pickleStep,
              step,
              TestStepResultStatus.UNKNOWN,
              this.options.statusIcons,
              this.options.theme,
              this.stream
            )
          ),
          GHERKIN_INDENT_LENGTH
        ).length
      })
    this.maxContentLengthByTestCaseStartedId.set(
      testCaseStarted.id,
      Math.max(scenarioLength, ...stepLengths)
    )

    let scenarioIndent = 0
    if (this.options.includeFeaturesAndRules) {
      scenarioIndent += GHERKIN_INDENT_LENGTH
      if (lineage.rule) {
        scenarioIndent += GHERKIN_INDENT_LENGTH
      }
    }
    this.scenarioIndentByTestCaseStartedId.set(testCaseStarted.id, scenarioIndent)
  }

  private getScenarioIndentBy(by: TestCaseStarted | TestStepFinished | Attachment) {
    if ('testCaseStartedId' in by && by.testCaseStartedId) {
      return this.scenarioIndentByTestCaseStartedId.get(by.testCaseStartedId) ?? 0
    } else if ('testCaseId' in by) {
      return this.scenarioIndentByTestCaseStartedId.get(by.id) ?? 0
    }
    return 0
  }

  private getMaxContentLengthBy(by: TestCaseStarted | TestStepFinished) {
    if ('testCaseStartedId' in by) {
      return this.maxContentLengthByTestCaseStartedId.get(by.testCaseStartedId) ?? 0
    }
    return this.maxContentLengthByTestCaseStartedId.get(by.id) ?? 0
  }

  private handleTestCaseStarted(testCaseStarted: TestCaseStarted) {
    const { pickle, location, scenario, rule, feature } = this.resolveScenario(testCaseStarted)
    const scenarioIndent = this.getScenarioIndentBy(testCaseStarted)
    const maxContentLength = this.getMaxContentLengthBy(testCaseStarted)

    this.printFeatureLine(feature)
    this.printRuleLine(rule)
    this.println()
    this.printTags(pickle, scenarioIndent)
    this.printScenarioLine(pickle, scenario, location, scenarioIndent, maxContentLength)
  }

  private printFeatureLine(feature: Feature) {
    if (this.options.includeFeaturesAndRules && !this.encounteredFeaturesAndRules.has(feature)) {
      this.println()
      this.println(formatFeatureTitle(feature, this.options.theme, this.stream))
    }
    this.encounteredFeaturesAndRules.add(feature)
  }

  private printRuleLine(rule: Rule | undefined) {
    if (rule) {
      if (this.options.includeFeaturesAndRules && !this.encounteredFeaturesAndRules.has(rule)) {
        this.println()
        this.println(
          indent(formatRuleTitle(rule, this.options.theme, this.stream), GHERKIN_INDENT_LENGTH)
        )
      }
      this.encounteredFeaturesAndRules.add(rule)
    }
  }

  private printTags(pickle: Pickle, scenarioIndent: number) {
    const output = formatPickleTags(pickle, this.options.theme, this.stream)
    if (output) {
      this.println(indent(output, scenarioIndent))
    }
  }

  private printScenarioLine(
    pickle: Pickle,
    scenario: Scenario,
    location: Location | undefined,
    scenarioIndent: number,
    maxContentLength: number
  ) {
    this.printGherkinLine(
      formatPickleTitle(pickle, scenario, this.options.theme, this.stream),
      formatPickleLocation(pickle, location, this.options.theme, this.stream),
      scenarioIndent,
      maxContentLength
    )
  }

  private handleTestStepFinished(testStepFinished: TestStepFinished) {
    const scenarioIndent = this.getScenarioIndentBy(testStepFinished)
    const maxContentLength = this.getMaxContentLengthBy(testStepFinished)
    const resolved = this.resolveStep(testStepFinished)
    if (resolved) {
      const { testStep, pickleStep, step, stepDefinition } = resolved
      this.printStepLine(
        testStepFinished,
        testStep,
        pickleStep,
        step,
        stepDefinition,
        scenarioIndent,
        maxContentLength
      )
      this.printStepArgument(pickleStep, scenarioIndent)
    }
    this.printError(testStepFinished, scenarioIndent)
  }

  private printStepLine(
    testStepFinished: TestStepFinished,
    testStep: TestStep,
    pickleStep: PickleStep,
    step: Step,
    stepDefinition: StepDefinition | undefined,
    scenarioIndent: number,
    maxContentLength: number
  ) {
    this.printGherkinLine(
      indent(
        formatStepTitle(
          testStep,
          pickleStep,
          step,
          testStepFinished.testStepResult.status,
          this.options.statusIcons,
          this.options.theme,
          this.stream
        ),
        GHERKIN_INDENT_LENGTH
      ),
      formatStepLocation(stepDefinition, this.options.theme, this.stream),
      scenarioIndent,
      maxContentLength
    )
  }

  private printStepArgument(pickleStep: PickleStep, scenarioIndent: number) {
    const content = formatStepArgument(pickleStep, this.options.theme, this.stream)
    if (content) {
      this.println(
        indent(
          content,
          scenarioIndent +
            (this.options.statusIcons ? GHERKIN_INDENT_LENGTH : 0) +
            GHERKIN_INDENT_LENGTH +
            STEP_ARGUMENT_INDENT_LENGTH
        )
      )
    }
  }

  private printGherkinLine(
    title: string,
    location: string | undefined,
    indentBy: number,
    maxContentLength: number
  ) {
    let output = title
    if (location) {
      const padding = maxContentLength - unstyled(title).length
      output += indent(location, padding + 1)
    }
    this.println(indent(output, indentBy))
  }

  private printError(testStepFinished: TestStepFinished, scenarioIndent: number) {
    const content = formatError(testStepFinished.testStepResult, this.options.theme, this.stream)
    if (content) {
      this.println(
        indent(
          content,
          scenarioIndent +
            (this.options.statusIcons ? GHERKIN_INDENT_LENGTH : 0) +
            GHERKIN_INDENT_LENGTH +
            ERROR_INDENT_LENGTH
        )
      )
    }
  }

  private handleAttachment(attachment: Attachment) {
    const scenarioIndent = this.getScenarioIndentBy(attachment)
    const content = formatAttachment(attachment, this.options.theme, this.stream)
    this.println(
      pad(
        indent(
          content,
          scenarioIndent +
            (this.options.statusIcons ? GHERKIN_INDENT_LENGTH : 0) +
            GHERKIN_INDENT_LENGTH +
            ATTACHMENT_INDENT_LENGTH
        )
      )
    )
  }
}
