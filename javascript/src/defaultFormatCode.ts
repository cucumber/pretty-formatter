import type { Snippet } from '@cucumber/messages'

import type { FormatCodeFunction } from './types'

export const defaultFormatCode: FormatCodeFunction = (snippet: Snippet) => snippet.code
