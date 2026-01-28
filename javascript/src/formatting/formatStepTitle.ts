import { PickleStep, Step, TestStep, TestStepResultStatus } from '@cucumber/messages'

import { TextBuilder } from '../TextBuilder'
import { Theme } from '../types'

export function formatStepTitle(
  testStep: TestStep,
  pickleStep: PickleStep,
  step: Step,
  status: TestStepResultStatus,
  useStatusIcon: boolean,
  theme: Theme,
  stream: NodeJS.WritableStream
): string {
  const builder = new TextBuilder(stream)
  if (useStatusIcon) {
    builder.append(theme.status?.icon?.[status] || ' ', theme.status?.all?.[status]).space()
  }
  return builder
    .append(
      new TextBuilder(stream)
        .append(step.keyword, theme.step?.keyword)
        // step keyword includes a trailing space
        .append(formatStepText(testStep, pickleStep, theme, stream))
        .build(theme.status?.all?.[status])
    )
    .build()
}

function formatStepText(
  testStep: TestStep,
  pickleStep: PickleStep,
  theme: Theme,
  stream: NodeJS.WritableStream
): string {
  const builder = new TextBuilder(stream)
  const stepMatchArgumentsLists = testStep.stepMatchArgumentsLists
  if (stepMatchArgumentsLists && stepMatchArgumentsLists.length === 1) {
    const stepMatchArguments = stepMatchArgumentsLists[0].stepMatchArguments
    let currentIndex = 0
    stepMatchArguments.forEach((argument) => {
      const group = argument.group
      // Ignore absent values, or groups without a start
      if (group.value !== undefined && group.start !== undefined) {
        const text = pickleStep.text.slice(currentIndex, group.start)
        currentIndex = group.start + group.value.length
        builder.append(text, theme.step?.text).append(group.value, theme.step?.argument)
      }
    })
    if (currentIndex != pickleStep.text.length) {
      const remainder = pickleStep.text.slice(currentIndex)
      builder.append(remainder, theme.step?.text)
    }
  } else {
    builder.append(pickleStep.text, theme.step?.text)
  }
  return builder.build()
}
