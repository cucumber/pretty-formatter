import {
  Attachment,
  Envelope,
  Location,
  Pickle,
  PickleStep,
  Scenario,
  Step,
  StepDefinition,
  TestCaseStarted,
  TestStepFinished,
} from '@cucumber/messages'
import { Query } from '@cucumber/query'

import {
  ATTACHMENT_INDENT_LENGTH,
  ensure,
  ERROR_INDENT_LENGTH,
  formatAttachment,
  formatError,
  formatPickleLocation,
  formatPickleTags,
  formatPickleTitle,
  formatStepArgument,
  formatStepLocation,
  formatStepTitle,
  indent,
  pad,
  STEP_ARGUMENT_INDENT_LENGTH,
  STEP_INDENT_LENGTH,
} from './helpers.js'

interface Options {
  includeFeaturesAndRules?: boolean
}

export class PrettyPrinter {
  private readonly query: Query = new Query()
  private readonly maxContentLengthByTestCaseStartedId: Map<string, number> = new Map<
    string,
    number
  >()
  private readonly writeln: (content: string) => void
  private options: Required<Options>

  constructor(
    private readonly write: (content: string) => void,
    options: Options = {}
  ) {
    this.writeln = (content: string) => this.write(`${content}\n`)
    this.options = {
      includeFeaturesAndRules: false,
      ...options,
    }
  }

  update(message: Envelope) {
    this.query.update(message)

    if (message.testCaseStarted) {
      this.write('\n')
      this.preCalculateMaxContentLength(message.testCaseStarted)
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
    const lineage = ensure(this.query.findLineageBy(pickle), 'Lineage must exist for Pickle')
    const scenario = ensure(lineage.scenario, 'Scenario must exist for Lineage')
    const location = this.query.findLocationOf(pickle)
    return {
      pickle,
      scenario,
      location,
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

  private preCalculateMaxContentLength(testCaseStarted: TestCaseStarted) {
    const pickle = ensure(
      this.query.findPickleBy(testCaseStarted),
      'Pickle must exist for TestCaseStarted'
    )
    const lineage = ensure(this.query.findLineageBy(pickle), 'Lineage must exist for Pickle')
    const scenario = ensure(lineage.scenario, 'Scenario must exist for Lineage')
    const scenarioLength = formatPickleTitle(pickle, scenario).length
    const stepLengths = pickle.steps.map((pickleStep) => {
      const step = ensure(this.query.findStepBy(pickleStep), 'Step must exist for PickleStep')
      return STEP_INDENT_LENGTH + formatStepTitle(pickleStep, step).length
    })
    this.maxContentLengthByTestCaseStartedId.set(
      testCaseStarted.id,
      Math.max(scenarioLength, ...stepLengths)
    )
  }

  private getMaxContentLengthBy(by: TestCaseStarted | TestStepFinished) {
    if ('testCaseStartedId' in by) {
      return this.maxContentLengthByTestCaseStartedId.get(by.testCaseStartedId) ?? 0
    }
    return this.maxContentLengthByTestCaseStartedId.get(by.id) ?? 0
  }

  private handleTestCaseStarted(testCaseStarted: TestCaseStarted) {
    const { pickle, scenario, location } = this.resolveScenario(testCaseStarted)
    const maxContentLength = this.getMaxContentLengthBy(testCaseStarted)

    this.printTags(pickle)
    this.printScenarioLine(pickle, scenario, location, maxContentLength)
  }

  private printTags(pickle: Pickle) {
    const output = formatPickleTags(pickle)
    if (output) {
      this.writeln(output)
    }
  }

  private printScenarioLine(
    pickle: Pickle,
    scenario: Scenario,
    location: Location | undefined,
    maxContentLength: number
  ) {
    this.printGherkinLine(
      formatPickleTitle(pickle, scenario),
      formatPickleLocation(pickle, location),
      0,
      maxContentLength
    )
  }

  private handleTestStepFinished(testStepFinished: TestStepFinished) {
    const resolved = this.resolveStep(testStepFinished)
    if (resolved) {
      const { pickleStep, step, stepDefinition } = resolved
      const maxContentLength = this.getMaxContentLengthBy(testStepFinished)
      this.printStepLine(pickleStep, step, stepDefinition, maxContentLength)
      this.printStepArgument(pickleStep)
    }
    this.printError(testStepFinished)
  }

  private printStepLine(
    pickleStep: PickleStep,
    step: Step,
    stepDefinition: StepDefinition | undefined,
    maxContentLength: number
  ) {
    this.printGherkinLine(
      formatStepTitle(pickleStep, step),
      formatStepLocation(stepDefinition),
      STEP_INDENT_LENGTH,
      maxContentLength
    )
  }

  private printStepArgument(pickleStep: PickleStep) {
    const content = formatStepArgument(pickleStep)
    if (content) {
      this.writeln(indent(content, STEP_INDENT_LENGTH + STEP_ARGUMENT_INDENT_LENGTH))
    }
  }

  private printGherkinLine(
    title: string,
    location: string | undefined,
    indentBy: number,
    maxContentLength: number
  ) {
    let output = indent(title, indentBy)
    if (location) {
      const padding = maxContentLength - output.length
      output += `${' '.repeat(padding)} # ${location}`
    }
    this.writeln(output)
  }

  private printError(testStepFinished: TestStepFinished) {
    const content = formatError(testStepFinished.testStepResult)
    if (content) {
      this.writeln(indent(content, STEP_INDENT_LENGTH + ERROR_INDENT_LENGTH))
    }
  }

  private handleAttachment(attachment: Attachment) {
    const content = formatAttachment(attachment)
    this.writeln(pad(indent(content, STEP_INDENT_LENGTH + ATTACHMENT_INDENT_LENGTH)))
  }
}
