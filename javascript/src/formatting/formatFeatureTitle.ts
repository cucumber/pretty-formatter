import { Feature } from '@cucumber/messages'

import { TextBuilder } from '../TextBuilder'
import { Theme } from '../types'

export function formatFeatureTitle(
  feature: Feature,
  theme: Theme,
  stream: NodeJS.WritableStream
): string {
  return new TextBuilder(stream)
    .append(feature.keyword + ':', theme.feature?.keyword)
    .space()
    .append(feature.name, theme.feature?.name)
    .build(theme.feature?.all)
}
