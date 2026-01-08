import fs from 'node:fs'
import * as path from 'node:path'
import { Writable } from 'node:stream'
import { pipeline } from 'node:stream/promises'

import { NdjsonToMessageStream } from '@cucumber/message-streams'
import { Envelope } from '@cucumber/messages'
import { Query } from '@cucumber/query'
import { expect } from 'chai'
import { globbySync } from 'globby'

import { SummaryPrinter } from './SummaryPrinter'
import { CUCUMBER_THEME } from './theme'
import type { SummaryOptions } from './types'

const updateExpectedFiles = process.env.UPDATE_EXPECTED_FILES === 'true'

describe('SummaryPrinter', async () => {
  const ndjsonFiles = globbySync(`*.ndjson`, {
    cwd: path.join(__dirname, '..', '..', 'testdata', 'src'),
    absolute: true,
  })

  const variants: ReadonlyArray<{ name: string; options: SummaryOptions }> = [
    {
      name: 'cucumber',
      options: {
        theme: CUCUMBER_THEME,
      },
    },
    {
      name: 'plain',
      options: {
        theme: {},
      },
    },
    {
      name: 'exclude-attachments',
      options: {
        theme: {},
        includeAttachments: false,
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
          const query = new Query()
          let content = ''
          const printer = new SummaryPrinter(
            query,
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
                query.update(envelope)
                callback()
              },
            })
          )

          printer.printSummary()

          const expectedPath = ndjsonFile.replace('.ndjson', `.${name}.summary.log`)
          if (updateExpectedFiles) {
            fs.writeFileSync(expectedPath, content, {
              encoding: 'utf-8',
            })
          }
          const expectedOutput = fs.readFileSync(expectedPath, {
            encoding: 'utf-8',
          })

          expect(content).to.eq(expectedOutput)
        })
      }
    })
  }
})
