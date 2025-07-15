import { styleText } from 'node:util'

import { Style } from './types.js'

export class TextBuilder {
  private text = ''

  private applyStyle(value: string, style?: Style) {
    if (!style) {
      return value
    }
    if (style === 'default') {
      // util.styleText doesn't support 'default' style
      return `\u001b[39m${value}\u001b[0m`
    }
    return styleText(style, value, { validateStream: false })
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
