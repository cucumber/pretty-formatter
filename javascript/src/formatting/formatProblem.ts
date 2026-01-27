import { TextBuilder } from '../TextBuilder'
import { Theme } from '../types'
import { ProblemType } from '../utils'

const PREFIXES: Record<ProblemType, string> = {
  [ProblemType.PARAMETER_TYPE]: 'Undefined parameter type',
  [ProblemType.GLOBAL_HOOK]: 'Global hook',
  [ProblemType.TEST_CASE]: 'Scenario',
  [ProblemType.TEST_RUN]: 'Test run',
}

export function formatProblem(
  type: ProblemType,
  details: string,
  theme: Theme,
  stream: NodeJS.WritableStream
): string {
  return new TextBuilder(stream)
    .append(PREFIXES[type] + ':', theme.affix)
    .space()
    .append(details)
    .build()
}
