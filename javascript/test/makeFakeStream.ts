export function makeFakeStream(write: (content: string) => void): NodeJS.WritableStream {
  return {
    _writableState: {},
    isTTY: true,
    getColorDepth: () => 3,
    write,
  } as unknown as NodeJS.WritableStream
}
