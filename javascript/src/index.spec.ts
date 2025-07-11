import fs from 'node:fs'
import * as path from 'node:path'
import { Writable } from 'node:stream'
import { pipeline } from 'node:stream/promises'

import { NdjsonToMessageStream } from '@cucumber/message-streams'
import { Envelope } from '@cucumber/messages'
import { expect } from 'chai'
import { globbySync } from 'globby'

import formatter from './index.js'

describe('Acceptance Tests', async function () {
  this.timeout(10_000)

  const ndjsonFiles = globbySync(`*.ndjson`, {
    cwd: new URL(path.join(path.dirname(import.meta.url), '../../testdata')),
    absolute: true,
  })

  const variants = ['exclude-features-and-rules']

  for (const variant of variants) {
    describe(variant, () => {
      for (const ndjsonFile of ndjsonFiles) {
        const [suiteName] = path.basename(ndjsonFile).split('.')

        it(suiteName, async () => {
          let emit: (message: Envelope) => void
          let content = ''
          formatter.formatter({
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

          const expectedOutput = fs.readFileSync(ndjsonFile.replace('.ndjson', `.${variant}.log`), {
            encoding: 'utf-8',
          })
          expect(content).to.eq(expectedOutput)
        })
      }
    })
  }
}).timeout('5s')
