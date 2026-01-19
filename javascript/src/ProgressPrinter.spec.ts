import fs from 'node:fs'
import * as path from 'node:path'
import { Writable } from 'node:stream'
import { pipeline } from 'node:stream/promises'

import { NdjsonToMessageStream } from '@cucumber/message-streams'
import { Envelope } from '@cucumber/messages'
import { expect } from 'chai'
import { globbySync } from 'globby'

import { ProgressPrinter } from './ProgressPrinter'
import { CUCUMBER_THEME, PLAIN_THEME } from './theme'
import type { ProgressOptions } from './types'

describe('ProgressPrinter', async () => {
  const ndjsonFiles = globbySync(`*.ndjson`, {
    cwd: path.join(__dirname, '..', '..', 'testdata', 'src'),
    absolute: true,
  })

  const variants: ReadonlyArray<{ name: string; options: ProgressOptions }> = [
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
          const printer = new ProgressPrinter({
            stream: fakeStream,
            print: (chunk) => {
              content += chunk
            },
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

          const expectedOutput = fs.readFileSync(
            ndjsonFile.replace('.ndjson', `.${name}.progress.log`),
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
    it('should append a summary when the option is enabled', async () => {
      let content = ''
      const printer = new ProgressPrinter({
        stream: fakeStream,
        print: (chunk) => {
          content += chunk
        },
        options: {
          theme: CUCUMBER_THEME,
          summarise: true,
        },
      })

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

      const expectedProgress = fs.readFileSync(
        ndjsonFile.replace('.ndjson', `.cucumber.progress.log`),
        { encoding: 'utf-8' }
      )
      const expectedSummary = fs.readFileSync(
        ndjsonFile.replace('.ndjson', `.cucumber.summary.log`),
        { encoding: 'utf-8' }
      )

      expect(content).to.eq(expectedProgress + expectedSummary)
    })
  })
})
