import type { WriteStream } from 'node:tty'

type TerminalWriter = (...args: Array<unknown>) => boolean
type Mode = 'passthrough' | 'suppress'

export class InterferenceInterceptor {
  private readonly acquired: Map<WriteStream, TerminalWriter> = new Map()
  private writing = false

  constructor(
    private readonly mode: Mode = 'passthrough',
    private readonly streams: Iterable<WriteStream> = []
  ) {}

  acquire() {
    if (this.mode === 'passthrough') {
      return
    }

    for (const stream of this.streams) {
      const original = stream.write.bind(stream) as TerminalWriter
      stream.write = (...args) => {
        if (this.writing) {
          return original(...args)
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
      this.acquired.delete(stream)
    }
  }
}
