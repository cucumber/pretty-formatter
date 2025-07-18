import type { Theme } from './types.js'
import { TestStepResultStatus } from './types.js'

export const CUCUMBER_THEME: Theme = {
  attachment: 'blue',
  feature: {
    keyword: 'bold',
  },
  location: 'blackBright',
  status: {
    all: {
      [TestStepResultStatus.AMBIGUOUS]: 'red',
      [TestStepResultStatus.FAILED]: 'red',
      [TestStepResultStatus.PASSED]: 'green',
      [TestStepResultStatus.PENDING]: 'yellow',
      [TestStepResultStatus.SKIPPED]: 'cyan',
      [TestStepResultStatus.UNDEFINED]: 'yellow',
    },
    icon: {
      [TestStepResultStatus.AMBIGUOUS]: '✘',
      [TestStepResultStatus.FAILED]: '✘',
      [TestStepResultStatus.PASSED]: '✔',
      [TestStepResultStatus.PENDING]: '■',
      [TestStepResultStatus.SKIPPED]: '↷',
      [TestStepResultStatus.UNDEFINED]: '■',
      [TestStepResultStatus.UNKNOWN]: ' ',
    },
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
}
