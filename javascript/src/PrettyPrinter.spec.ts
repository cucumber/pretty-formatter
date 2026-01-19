import fs from 'node:fs'
import * as path from 'node:path'
import { Writable } from 'node:stream'
import { pipeline } from 'node:stream/promises'

import { NdjsonToMessageStream } from '@cucumber/message-streams'
import { Envelope } from '@cucumber/messages'
import { expect } from 'chai'
import { globbySync } from 'globby'

import { makeFakeStream } from '../test/makeFakeStream'
import { PrettyPrinter } from './PrettyPrinter'
import { CUCUMBER_THEME, DEMO_THEME, NONE_THEME, PLAIN_THEME } from './theme'
import type { PrettyOptions } from './types'

const updateExpectedFiles = process.env.UPDATE_EXPECTED_FILES === 'true'

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
        theme: NONE_THEME,
      },
    },
    {
      name: 'exclude-attachments',
      options: {
        includeAttachments: false,
        includeFeatureLine: true,
        includeRuleLine: true,
        useStatusIcon: false,
        theme: NONE_THEME,
      },
    },
    {
      name: 'none',
      options: {
        includeAttachments: true,
        includeFeatureLine: true,
        includeRuleLine: true,
        useStatusIcon: false,
        theme: NONE_THEME,
      },
    },
    {
      name: 'plain',
      options: {
        includeAttachments: true,
        includeFeatureLine: true,
        includeRuleLine: true,
        useStatusIcon: true,
        theme: PLAIN_THEME,
      },
    },
  ]

  for (const { name, options } of variants) {
    describe(name, () => {
      for (const ndjsonFile of ndjsonFiles) {
        const [suiteName] = path.basename(ndjsonFile).split('.')

        it(suiteName, async () => {
          let content = ''
          const stream = makeFakeStream((chunk) => (content += chunk))
          const printer = new PrettyPrinter({
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

          const expectedPath = ndjsonFile.replace('.ndjson', `.${name}.pretty.log`)
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

  describe('summarise', () => {
    it('should append a summary when the option is enabled', async () => {
      let content = ''
      const stream = makeFakeStream((chunk) => (content += chunk))
      const printer = new PrettyPrinter({
        stream,
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

      const expectedPretty = fs.readFileSync(
        ndjsonFile.replace('.ndjson', `.cucumber.pretty.log`),
        { encoding: 'utf-8' }
      )
      const expectedSummary = fs.readFileSync(
        ndjsonFile.replace('.ndjson', `.cucumber.summary.log`),
        { encoding: 'utf-8' }
      )

      expect(content).to.eq(expectedPretty + expectedSummary)
    })
  })
})
