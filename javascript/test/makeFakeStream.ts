import { WriteStream } from 'node:tty'

type FakeStream = WriteStream & { content: string }

class FakeStreamImpl {
  private _content = ''
  private _cursorOffset = 0

  readonly _writableState = {}
  readonly isTTY = true
  readonly columns = 80

  getColorDepth(): number {
    return 3
  }

  write(chunk: string): boolean {
    this._content += chunk
    this._cursorOffset = 0
    return true
  }

  moveCursor(dx: number, dy: number, callback?: () => void): boolean {
    this._cursorOffset = dy
    callback?.()
    return true
  }

  clearScreenDown(callback?: () => void): boolean {
    if (this._cursorOffset < 0) {
      const lines = this._content.split('\n')
      this._content = lines.slice(0, this._cursorOffset).join('\n')
    }
    this._cursorOffset = 0
    callback?.()
    return true
  }

  get content(): string {
    return this._content
  }
}

export function makeFakeStream(): FakeStream {
  return new FakeStreamImpl() as unknown as FakeStream
}
