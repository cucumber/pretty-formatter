import fs from 'node:fs'
import * as path from 'node:path'
import { Writable } from 'node:stream'
import { pipeline } from 'node:stream/promises'

import { NdjsonToMessageStream } from '@cucumber/message-streams'
import { Envelope, TestStepResultStatus } from '@cucumber/messages'
import { expect } from 'chai'
import { globbySync } from 'globby'

import { SummaryPrinter } from './SummaryPrinter.js'
import { CUCUMBER_THEME } from './theme.js'
import type { Options } from './types.js'

describe('SummaryPrinter', async () => {
  const ndjsonFiles = globbySync(`*.ndjson`, {
    cwd: path.join(import.meta.dirname, '..', '..', 'testdata', 'src'),
    absolute: true,
  })

  const variants: ReadonlyArray<{ name: string; options: Options }> = [
    {
      name: 'cucumber',
      options: {
        attachments: true,
        featuresAndRules: true,
        theme: CUCUMBER_THEME,
      },
    },
    {
      name: 'plain',
      options: {
        attachments: true,
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
          let content = ''
          const printer = new SummaryPrinter(
            fakeStream,
            (chunk) => {
              content += chunk
            },
            {
              attachments: true,
              featuresAndRules: true,
              theme: CUCUMBER_THEME,
              ...options,
            }
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
            ndjsonFile.replace('.ndjson', `.${name}.summary.log`),
            {
              encoding: 'utf-8',
            }
          )

          expect(content).to.eq(expectedOutput)
        })
      }
    })
  }
})
