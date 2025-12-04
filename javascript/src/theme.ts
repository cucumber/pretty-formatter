import { DEFAULT_PROGRESS_ICONS, DEFAULT_STATUS_COLORS, DEFAULT_STATUS_ICONS } from './helpers'
import type { Theme } from './types'

export const CUCUMBER_THEME: Theme = {
  attachment: 'blue',
  feature: {
    keyword: 'bold',
  },
  location: 'blackBright',
  status: {
    all: { ...DEFAULT_STATUS_COLORS },
    icon: { ...DEFAULT_STATUS_ICONS },
    progress: { ...DEFAULT_PROGRESS_ICONS },
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
