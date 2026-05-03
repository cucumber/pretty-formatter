import type { WriteStream } from 'node:tty'

type TerminalWriter = (...args: Array<unknown>) => boolean

export type InterferenceConfig =
  | { mode: 'passthrough' }
  | { mode: 'suppress'; streams: ReadonlyArray<WriteStream> }

export class InterferenceInterceptor {
  private readonly acquired: Map<WriteStream, TerminalWriter> = new Map()
  private writing = false

  constructor(private readonly config: InterferenceConfig = { mode: 'passthrough' }) {}

  acquire() {
    if (this.config.mode === 'passthrough') {
      return
    }

    for (const stream of this.config.streams) {
      if (this.acquired.has(stream)) {
        continue
      }
      const original = stream.write.bind(stream) as TerminalWriter
      stream.write = (...args) => {
        if (this.writing) {
          return original(...args)
        }
        const callback = args[2] ?? args[1]
        if (typeof callback === 'function') {
          process.nextTick(callback as () => void)
        }
        return true
      }
      this.acquired.set(stream, original)
    }
  }

  bypass<T>(callback: () => T): T {
    this.writing = true
    try {
      return callback()
    } finally {
      this.writing = false
    }
  }

  release() {
    for (const [stream, original] of this.acquired.entries()) {
      stream.write = original
    }
    this.acquired.clear()
  }
}
