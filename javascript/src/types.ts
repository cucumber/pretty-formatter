import { styleText } from 'node:util'

import { TestStepResultStatus } from '@cucumber/messages'

export type Style = Parameters<typeof styleText>[0] | 'default'

export interface Theme {
  attachment?: Style
  dataTable?: {
    all?: Style
    border?: Style
    content?: Style
  }
  docString?: {
    all?: Style
    content?: Style
    delimiter?: Style
    mediaType?: Style
  }
  feature?: {
    all?: Style
    keyword?: Style
    name?: Style
  }
  location?: Style
  rule?: {
    all?: Style
    keyword?: Style
    name?: Style
  }
  scenario?: {
    all?: Style
    keyword?: Style
    name?: Style
  }
  status?: {
    all?: Partial<Record<TestStepResultStatus, Style>>
    icon?: Partial<Record<TestStepResultStatus, string>>
  }
  step?: {
    argument?: Style
    keyword?: Style
    text?: Style
  }
  tag?: Style
}

export interface Options {
  includeFeaturesAndRules?: boolean
  theme?: Theme
}
