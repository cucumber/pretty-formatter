import fs from 'node:fs'
import * as path from 'node:path'
import { Writable } from 'node:stream'
import { pipeline } from 'node:stream/promises'

import { NdjsonToMessageStream } from '@cucumber/message-streams'
import { Envelope } from '@cucumber/messages'
import { expect } from 'chai'
import { globbySync } from 'globby'

import { makeFakeStream } from '../test/makeFakeStream'
import { SummaryPrinter } from './SummaryPrinter'
import { CUCUMBER_THEME, PLAIN_THEME } from './theme'
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
        theme: PLAIN_THEME,
      },
    },
    {
      name: 'exclude-attachments',
      options: {
        theme: PLAIN_THEME,
        includeAttachments: false,
      },
    },
  ]

  for (const { name, options } of variants) {
    describe(name, () => {
      for (const ndjsonFile of ndjsonFiles) {
        const [suiteName] = path.basename(ndjsonFile).split('.')

        it(suiteName, async () => {
          const stream = makeFakeStream()
          const printer = new SummaryPrinter({
            stream,
            options,
          })

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

          const expectedPath = ndjsonFile.replace('.ndjson', `.${name}.summary.log`)
          if (updateExpectedFiles) {
            fs.writeFileSync(expectedPath, stream.content, {
              encoding: 'utf-8',
            })
          }
          const expectedOutput = fs.readFileSync(expectedPath, {
            encoding: 'utf-8',
          })

          expect(stream.content).to.eq(expectedOutput)
        })
      }
    })
  }
})
