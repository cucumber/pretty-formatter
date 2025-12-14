import fs from 'node:fs'
import * as path from 'node:path'
import { Writable } from 'node:stream'
import { pipeline } from 'node:stream/promises'

import { NdjsonToMessageStream } from '@cucumber/message-streams'
import { Envelope, TestStepResultStatus } from '@cucumber/messages'
import { expect } from 'chai'
import { globbySync } from 'globby'

import { PrettyPrinter } from './PrettyPrinter'
import { CUCUMBER_THEME } from './theme'
import type { PrettyOptions, Theme } from './types'

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

describe('PrettyPrinter', async () => {
  const ndjsonFiles = globbySync(`*.ndjson`, {
    cwd: path.join(__dirname, '..', '..', 'testdata', 'src'),
    absolute: true,
  })

  const variants: ReadonlyArray<{ name: string; options: PrettyOptions }> = [
    {
      name: 'cucumber',
      options: {
        includeAttachments: true,
        includeFeatureLine: true,
        includeRuleLine: true,
        theme: CUCUMBER_THEME,
        useStatusIcon: true,
      },
    },
    {
      name: 'demo',
      options: {
        includeAttachments: true,
        includeFeatureLine: true,
        includeRuleLine: true,
        theme: DEMO_THEME,
        useStatusIcon: false,
      },
    },
    {
      name: 'exclude-features-and-rules',
      options: {
        includeAttachments: true,
        includeFeatureLine: false,
        includeRuleLine: false,
        useStatusIcon: false,
        theme: {},
      },
    },
    {
      name: 'exclude-attachments',
      options: {
        includeAttachments: false,
        includeFeatureLine: true,
        includeRuleLine: true,
        useStatusIcon: false,
        theme: {},
      },
    },
    {
      name: 'none',
      options: {
        includeAttachments: true,
        includeFeatureLine: true,
        includeRuleLine: true,
        useStatusIcon: false,
        theme: {},
      },
    },
    {
      name: 'plain',
      options: {
        includeAttachments: true,
        includeFeatureLine: true,
        includeRuleLine: true,
        useStatusIcon: true,
        theme: {},
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
          let content = ''
          const printer = new PrettyPrinter(
            fakeStream,
            (chunk) => {
              content += chunk
            },
            options
          )

          await pipeline(
            fs.createReadStream(ndjsonFile, { encoding: 'utf-8' }),
            new NdjsonToMessageStream(),
            new Writable({
              objectMode: true,
              write(envelope: Envelope, _: BufferEncoding, callback) {
                printer.update(envelope)
                callback()
              },
            })
          )

          const expectedOutput = fs.readFileSync(
            ndjsonFile.replace('.ndjson', `.${name}.pretty.log`),
            {
              encoding: 'utf-8',
            }
          )

          expect(content).to.eq(expectedOutput)
        })
      }
    })
  }

  describe('summarise', () => {
    it('should append a summary on request', async () => {
      let content = ''
      const printer = new PrettyPrinter(
        fakeStream,
        (chunk) => {
          content += chunk
        },
        {
          theme: {},
        }
      )

      const ndjsonFile = path.join(__dirname, '..', '..', 'testdata', 'src', 'minimal.ndjson')
      await pipeline(
        fs.createReadStream(ndjsonFile, {
          encoding: 'utf-8',
        }),
        new NdjsonToMessageStream(),
        new Writable({
          objectMode: true,
          write(envelope: Envelope, _: BufferEncoding, callback) {
            printer.update(envelope)
            callback()
          },
        })
      )

      printer.summarise()

      const expectedSummary = fs.readFileSync(ndjsonFile.replace('.ndjson', `.plain.summary.log`), {
        encoding: 'utf-8',
      })

      expect(content).to.have.string(expectedSummary)
    })
  })
})
