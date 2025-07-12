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
    all: 'default',
    keyword: 'bold',
    name: 'italic',
  },
  location: 'blackBright',
  status: {
    [TestStepResultStatus.AMBIGUOUS]: 'red',
    [TestStepResultStatus.FAILED]: 'red',
    [TestStepResultStatus.PASSED]: 'green',
    [TestStepResultStatus.PENDING]: 'yellow',
    [TestStepResultStatus.SKIPPED]: 'cyan',
    [TestStepResultStatus.UNDEFINED]: 'yellow',
    [TestStepResultStatus.UNKNOWN]: [],
  },
  rule: {
    all: 'default',
    keyword: 'bold',
    name: 'italic',
  },
  scenario: {
    all: 'default',
    keyword: 'bold',
    name: 'italic',
  },
  step: {
    argument: 'bold',
    keyword: 'bold',
    text: 'italic',
  },
  tag: ['yellow', 'bold'],
}
