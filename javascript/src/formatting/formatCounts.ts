import { TestStepResultStatus } from '@cucumber/messages'

import { TextBuilder } from '../TextBuilder'
import { Theme } from '../types'
import { ORDERED_STATUSES } from '../utils'

export function formatCounts(
  suffix: string,
  counts: Partial<Record<TestStepResultStatus, number>>,
  theme: Theme,
  stream: NodeJS.WritableStream
): string {
  const builder = new TextBuilder(stream)
  const total = Object.values(counts).reduce((prev, curr) => prev + curr, 0)
  builder.append(`${total} ${suffix}`)
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
