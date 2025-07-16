import { TestStepResultStatus } from '@cucumber/messages'

import { Theme } from './types.js'

export const CUCUMBER_THEME: Theme = {
  attachment: 'blue',
  feature: {
    keyword: 'bold',
  },
  location: 'blackBright',
  status: {
    [TestStepResultStatus.AMBIGUOUS]: 'red',
    [TestStepResultStatus.FAILED]: 'red',
    [TestStepResultStatus.PASSED]: 'green',
    [TestStepResultStatus.PENDING]: 'yellow',
    [TestStepResultStatus.SKIPPED]: 'cyan',
    [TestStepResultStatus.UNDEFINED]: 'yellow',
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
