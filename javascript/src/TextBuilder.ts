import { styleText } from 'node:util'

import { Style } from './types'

export class TextBuilder {
  private text = ''

  constructor(private readonly stream: NodeJS.WritableStream) {}

  private applyStyle(value: string, style?: Style) {
    if (!style) {
      return value
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
