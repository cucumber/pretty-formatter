import { TestStepResultStatus } from '@cucumber/messages'

import { Style, Theme } from './types'

const CUCUMBER_STATUS_COLORS: Record<TestStepResultStatus, Style> = {
  [TestStepResultStatus.AMBIGUOUS]: 'magenta',
  [TestStepResultStatus.FAILED]: 'red',
  [TestStepResultStatus.PASSED]: 'green',
  [TestStepResultStatus.PENDING]: 'cyan',
  [TestStepResultStatus.SKIPPED]: 'yellow',
  [TestStepResultStatus.UNDEFINED]: 'blue',
  [TestStepResultStatus.UNKNOWN]: 'gray',
}

const CUCUMBER_STATUS_ICONS: Record<TestStepResultStatus, string> = {
  [TestStepResultStatus.AMBIGUOUS]: '✘',
  [TestStepResultStatus.FAILED]: '✘',
  [TestStepResultStatus.PASSED]: '✔',
  [TestStepResultStatus.PENDING]: '■',
  [TestStepResultStatus.SKIPPED]: '↷',
  [TestStepResultStatus.UNDEFINED]: '■',
  [TestStepResultStatus.UNKNOWN]: ' ',
}

const CUCUMBER_PROGRESS_ICONS: Record<TestStepResultStatus, string> = {
  [TestStepResultStatus.AMBIGUOUS]: 'A',
  [TestStepResultStatus.FAILED]: 'F',
  [TestStepResultStatus.PASSED]: '.',
  [TestStepResultStatus.PENDING]: 'P',
  [TestStepResultStatus.SKIPPED]: '-',
  [TestStepResultStatus.UNDEFINED]: 'U',
  [TestStepResultStatus.UNKNOWN]: '?',
}

export const CUCUMBER_THEME: Theme = {
  attachment: 'blue',
  feature: {
    keyword: 'bold',
  },
  location: 'blackBright',
  status: {
    all: CUCUMBER_STATUS_COLORS,
    icon: CUCUMBER_STATUS_ICONS,
    progress: CUCUMBER_PROGRESS_ICONS,
  },
  rule: {
    keyword: 'bold',
  },
  scenario: {
    keyword: 'bold',
  },
  step: {
    argument: 'bold',
    keyword: 'bold',
  },
  symbol: {
    bullet: '•',
  },
}

export const DEMO_THEME: Theme = {
  attachment: 'blue',
  dataTable: {
    all: 'blackBright',
    border: 'dim',
    content: 'italic',
  },
  docString: {
    all: 'blackBright',
    content: 'italic',
    delimiter: 'dim',
    mediaType: 'bold',
  },
  feature: {
    all: 'bgBlue',
    keyword: 'bold',
    name: 'italic',
  },
  location: 'blackBright',
  status: {
    all: CUCUMBER_STATUS_COLORS,
  },
  rule: {
    all: 'bgBlue',
    keyword: 'bold',
    name: 'italic',
  },
  scenario: {
    all: 'bgBlue',
    keyword: 'bold',
    name: 'italic',
  },
  step: {
    argument: 'bold',
    keyword: 'bold',
    text: 'italic',
  },
  tag: ['yellow', 'bold'],
  symbol: {
    bullet: '•',
  },
}

export const PLAIN_THEME: Theme = {
  status: {
    icon: CUCUMBER_STATUS_ICONS,
    progress: CUCUMBER_PROGRESS_ICONS,
  },
  symbol: {
    bullet: '-',
  },
}

export const NONE_THEME: Theme = {}
