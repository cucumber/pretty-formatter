import type { PickleTag, Tag } from '@cucumber/messages'

import { TextBuilder } from '../TextBuilder.js'
import type { Theme } from '../types.js'

export function formatTags(
  tags: ReadonlyArray<Tag | PickleTag>,
  theme: Theme,
  stream: NodeJS.WritableStream
): string {
  return new TextBuilder(stream).append(tags.map((tag) => `${tag.name}`).join(' ')).build(theme.tag)
}
