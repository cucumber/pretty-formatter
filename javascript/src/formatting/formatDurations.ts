import { Duration, TimeConversion } from '@cucumber/messages'
import { Duration as LuxonDuration } from 'luxon'

const DURATION_FORMAT = "m'm' s.S's'"

export function formatDurations(
  testRunDuration: Duration,
  executionDurations: ReadonlyArray<Duration>
): string {
  const testRunLuxon = LuxonDuration.fromMillis(
    TimeConversion.durationToMilliseconds(testRunDuration)
  )

  const executionLuxon = LuxonDuration.fromMillis(
    executionDurations.reduce((prev, curr) => prev + TimeConversion.durationToMilliseconds(curr), 0)
  )

  return `${testRunLuxon.toFormat(DURATION_FORMAT)} (${executionLuxon.toFormat(DURATION_FORMAT)} executing your code)`
}
