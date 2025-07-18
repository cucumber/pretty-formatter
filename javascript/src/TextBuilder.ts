import { styleText } from 'node:util'

import { Style } from './types.js'

export class TextBuilder {
  private readonly supportsColor: boolean
  private text = ''

  constructor(private readonly stream: NodeJS.WritableStream) {
    this.supportsColor = styleText('green', '-', { stream }) !== '-'
  }

  private applyStyle(value: string, style?: Style) {
    if (!style) {
      return value
    }
    if (style === 'default') {
      // util.styleText doesn't support 'default' style
      return this.supportsColor ? `\u001b[39m${value}\u001b[0m` : value
    }
    return styleText(style, value, { stream: this.stream })
  }

  space() {
    this.text += ' '
    return this
  }

  line() {
    this.text += '\n'
    return this
  }

  append(value: string | number, style?: Style) {
    this.text += this.applyStyle(value.toString(), style)
    return this
  }

  build(style?: Style, styleEachLine = false) {
    if (styleEachLine) {
      return this.text
        .split('\n')
        .map((line) => this.applyStyle(line, style))
        .join('\n')
    }
    return this.applyStyle(this.text, style)
  }
}
