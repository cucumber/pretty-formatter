import fs from 'node:fs'
import * as path from 'node:path'
import { Writable } from 'node:stream'
import { pipeline } from 'node:stream/promises'

import { NdjsonToMessageStream } from '@cucumber/message-streams'
import { Envelope, TestStepResultStatus } from '@cucumber/messages'
import { expect } from 'chai'
import { globbySync } from 'globby'

import type { Options, Theme } from './index.js'
import formatter, { CUCUMBER_THEME } from './index.js'

const DEMO_THEME: Theme = {
  attachment: 'blue',
  dataTable: {
    all: 'blackBright',
    border: 'dim',
    content: 'italic',
  },
  docString: {
    all: 'blackBright',
    content: 'italic',
    delimiter: 'dim',
    mediaType: 'bold',
  },
  feature: {
    all: 'bgBlue',
    keyword: 'bold',
    name: 'italic',
  },
  location: 'blackBright',
  status: {
    all: {
      [TestStepResultStatus.AMBIGUOUS]: 'red',
      [TestStepResultStatus.FAILED]: 'red',
      [TestStepResultStatus.PASSED]: 'green',
      [TestStepResultStatus.PENDING]: 'yellow',
      [TestStepResultStatus.SKIPPED]: 'cyan',
      [TestStepResultStatus.UNDEFINED]: 'yellow',
      [TestStepResultStatus.UNKNOWN]: [],
    },
  },
  rule: {
    all: 'bgBlue',
    keyword: 'bold',
    name: 'italic',
  },
  scenario: {
    all: 'bgBlue',
    keyword: 'bold',
    name: 'italic',
  },
  step: {
    argument: 'bold',
    keyword: 'bold',
    text: 'italic',
  },
  tag: ['yellow', 'bold'],
}

describe('Acceptance Tests', async function () {
  this.timeout(10_000)

  const ndjsonFiles = globbySync(`*.ndjson`, {
    cwd: path.join(import.meta.dirname, '..', '..', 'testdata'),
    absolute: true,
  })

  const variants: ReadonlyArray<{ name: string; options: Options }> = [
    {
      name: 'cucumber',
      options: {
        featuresAndRules: true,
        theme: CUCUMBER_THEME,
      },
    },
    {
      name: 'demo',
      options: {
        featuresAndRules: true,
        theme: DEMO_THEME,
      },
    },
    {
      name: 'exclude-features-and-rules',
      options: {
        featuresAndRules: false,
        theme: {},
      },
    },
    {
      name: 'none',
      options: {
        featuresAndRules: true,
        theme: {},
      },
    },
    {
      name: 'plain',
      options: {
        featuresAndRules: true,
        theme: {
          status: {
            icon: {
              [TestStepResultStatus.AMBIGUOUS]: '✘',
              [TestStepResultStatus.FAILED]: '✘',
              [TestStepResultStatus.PASSED]: '✔',
              [TestStepResultStatus.PENDING]: '■',
              [TestStepResultStatus.SKIPPED]: '↷',
              [TestStepResultStatus.UNDEFINED]: '■',
              [TestStepResultStatus.UNKNOWN]: ' ',
            },
          },
        },
      },
    },
  ]

  // just enough so Node.js internals consider it a color-supporting stream
  const fakeStream = {
    _writableState: {},
    isTTY: true,
    getColorDepth: () => 3,
  } as unknown as NodeJS.WritableStream

  for (const { name, options } of variants) {
    describe(name, () => {
      for (const ndjsonFile of ndjsonFiles) {
        const [suiteName] = path.basename(ndjsonFile).split('.')

        it(suiteName, async () => {
          let emit: (message: Envelope) => void
          let content = ''
          formatter.formatter({
            options,
            stream: fakeStream,
            on(type, handler) {
              emit = handler
            },
            write: (chunk) => {
              content += chunk
            },
          })

          await pipeline(
            fs.createReadStream(ndjsonFile, { encoding: 'utf-8' }),
            new NdjsonToMessageStream(),
            new Writable({
              objectMode: true,
              write(envelope: Envelope, _: BufferEncoding, callback) {
                emit(envelope)
                callback()
              },
            })
          )

          const expectedOutput = fs.readFileSync(ndjsonFile.replace('.ndjson', `.${name}.log`), {
            encoding: 'utf-8',
          })

          expect(content).to.eq(expectedOutput)
        })
      }
    })
  }
})
