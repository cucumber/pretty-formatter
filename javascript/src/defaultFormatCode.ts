import { Snippet } from '@cucumber/messages'

import { FormatCodeFunction } from './types'

export const defaultFormatCode: FormatCodeFunction = (snippet: Snippet) => snippet.code
