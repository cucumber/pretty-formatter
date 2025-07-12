import { expect } from 'chai'

import { indent, pad } from './helpers.js'

describe('helpers', () => {
  describe('indent', () => {
    it('should indent multiples lines by the given number', () => {
      const input = 'one\ntwo\nthree'
      expect(indent(input, 0)).to.equal('one\ntwo\nthree')
      expect(indent(input, 1)).to.equal(' one\n two\n three')
      expect(indent(input, 3)).to.equal('   one\n   two\n   three')
    })
  })

  describe('pad', () => {
    it('should pad string with newlines either side', () => {
      expect(pad('foo\nbar')).to.equal('\nfoo\nbar\n')
    })
  })
})
