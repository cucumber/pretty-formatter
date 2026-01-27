import { Location, Pickle } from '@cucumber/messages'

import { TextBuilder } from '../TextBuilder'
import { Theme } from '../types'

// TODO once Pickle.location is widely implemented, no need to pass location arg
export function formatPickleLocation(
  pickle: Pickle,
  location: Location | undefined,
  theme: Theme,
  stream: NodeJS.WritableStream
): string {
  const builder = new TextBuilder(stream).append('#').space().append(pickle.uri)
  if (location) {
    builder.append(':').append(location.line)
  }
  return builder.build(theme.location)
}
