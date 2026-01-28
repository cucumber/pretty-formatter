import { TestStepResultStatus } from '@cucumber/messages'

export function formatStatusName(status: TestStepResultStatus) {
  return `${status.charAt(0).toUpperCase() + status.slice(1).toLowerCase()}`
}
