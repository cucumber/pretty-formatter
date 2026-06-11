import type { Snippet } from '@cucumber/messages'

import type { FormatCodeFunction } from './types.js'

export const defaultFormatCode: FormatCodeFunction = (snippet: Snippet) => snippet.code
