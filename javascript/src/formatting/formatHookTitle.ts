import { Hook, HookType, TestStepResultStatus } from '@cucumber/messages'

import { TextBuilder } from '../TextBuilder'
import { Theme } from '../types'

const HOOK_TYPE_LABELS: Record<HookType, string> = {
  [HookType.BEFORE_TEST_RUN]: 'BeforeAll',
  [HookType.AFTER_TEST_RUN]: 'AfterAll',
  [HookType.BEFORE_TEST_CASE]: 'Before',
  [HookType.AFTER_TEST_CASE]: 'After',
  [HookType.BEFORE_TEST_STEP]: 'BeforeStep',
  [HookType.AFTER_TEST_STEP]: 'AfterStep',
}

export function formatHookTitle(
  hook: Hook | undefined,
  status: TestStepResultStatus,
  theme: Theme,
  stream: NodeJS.WritableStream
): string {
  const builder = new TextBuilder(stream).append(
    hook?.type ? HOOK_TYPE_LABELS[hook.type] : 'Hook',
    theme.step?.keyword
  )
  if (hook?.name) {
    builder.append(` (${hook.name})`, theme.step?.text)
  }
  return builder.build(theme.status?.all?.[status])
}
