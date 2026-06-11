import type { Rule } from '@cucumber/messages'

import { TextBuilder } from '../TextBuilder.js'
import type { Theme } from '../types.js'

export function formatRuleTitle(rule: Rule, theme: Theme, stream: NodeJS.WritableStream): string {
  return new TextBuilder(stream)
    .append(`${rule.keyword}:`, theme.rule?.keyword)
    .space()
    .append(rule.name, theme.rule?.name)
    .build(theme.rule?.all)
}
