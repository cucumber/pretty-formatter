import { expect } from 'chai'

import { TextBuilder } from './TextBuilder.js'

describe('TextBuilder', () => {
  // just enough so Node.js internals consider it a non color-supporting stream
  const fakeStream = {
    _writableState: {},
    isTTY: false,
  } as unknown as NodeJS.WritableStream

  it('skips "default" modifier when stream doesnt support color', () => {
    const builder = new TextBuilder(fakeStream)
    expect(builder.append('foo').build('default')).to.eq('foo')
  })
})
