import { UndefinedParameterType } from '@cucumber/messages'

export function formatUndefinedParameterType(upt: UndefinedParameterType): string {
  return `'${upt.name}' in '${upt.expression}'`
}
