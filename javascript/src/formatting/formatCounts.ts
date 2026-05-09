import type { TestStepResultStatus } from '@cucumber/messages'

import { TextBuilder } from '../TextBuilder'
import type { Theme } from '../types'
import { ORDERED_STATUSES } from '../utils'

export function formatCounts(
  noun: readonly [singular: string, plural: string],
  counts: Partial<Record<TestStepResultStatus, number>>,
  theme: Theme,
  stream: NodeJS.WritableStream
): string {
  const builder = new TextBuilder(stream)
  const total = Object.values(counts).reduce((prev, curr) => prev + curr, 0)
  builder.append(`${total} ${total === 1 ? noun[0] : noun[1]}`)
  if (total > 0) {
    let first = true
    builder.append(' (')
    for (const status of ORDERED_STATUSES) {
      const count = counts[status]
      if (count) {
        if (!first) {
          builder.append(', ')
        }
        builder.append(`${count} ${status.toLowerCase()}`, theme.status?.all?.[status])
        first = false
      }
    }
    builder.append(')')
  }
  return builder.build()
}
