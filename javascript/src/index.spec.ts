import fs from 'node:fs'
import * as path from 'node:path'
import { Writable } from 'node:stream'
import { pipeline } from 'node:stream/promises'

import { NdjsonToMessageStream } from '@cucumber/message-streams'
import { Envelope, TestStepResultStatus } from '@cucumber/messages'
import { expect } from 'chai'
import { globbySync } from 'globby'

import formatter, { Theme } from './index.js'
import { CUCUMBER_THEME } from './theme.js'
import type { Options } from './types.js'

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
    all: 'default',
    keyword: 'bold',
    name: 'italic',
  },
  location: 'blackBright',
  status: {
    [TestStepResultStatus.AMBIGUOUS]: 'red',
    [TestStepResultStatus.FAILED]: 'red',
    [TestStepResultStatus.PASSED]: 'green',
    [TestStepResultStatus.PENDING]: 'yellow',
    [TestStepResultStatus.SKIPPED]: 'cyan',
    [TestStepResultStatus.UNDEFINED]: 'yellow',
    [TestStepResultStatus.UNKNOWN]: [],
  },
  rule: {
    all: 'default',
    keyword: 'bold',
    name: 'italic',
  },
  scenario: {
    all: 'default',
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
    cwd: new URL(path.join(path.dirname(import.meta.url), '../../testdata')),
    absolute: true,
  })

  const variants: ReadonlyArray<{ name: string; options: Options }> = [
    {
      name: 'cucumber',
      options: {
        includeFeaturesAndRules: true,
        statusIcons: true,
        theme: CUCUMBER_THEME,
      },
    },
    {
      name: 'demo',
      options: {
        includeFeaturesAndRules: true,
        statusIcons: false,
        theme: DEMO_THEME,
      },
    },
    {
      name: 'exclude-features-and-rules',
      options: {
        includeFeaturesAndRules: false,
        statusIcons: false,
        theme: {},
      },
    },
    {
      name: 'none',
      options: {
        includeFeaturesAndRules: true,
        statusIcons: false,
        theme: {},
      },
    },
    {
      name: 'plain',
      options: {
        includeFeaturesAndRules: true,
        statusIcons: true,
        theme: {},
      },
    },
  ]

  for (const { name, options } of variants) {
    describe(name, () => {
      for (const ndjsonFile of ndjsonFiles) {
        const [suiteName] = path.basename(ndjsonFile).split('.')

        it(suiteName, async () => {
          let emit: (message: Envelope) => void
          let content = ''
          formatter.formatter({
            options,
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
}).timeout('5s')
