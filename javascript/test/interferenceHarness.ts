import fs from 'node:fs'
import path from 'node:path'
import { setTimeout } from 'node:timers/promises'

import type { Envelope } from '@cucumber/messages'

import { ProgressBarPrinter } from '../src'

const DEFAULT_NDJSON = path.join(__dirname, '..', '..', 'testdata', 'src', 'all-statuses.ndjson')
const DELAY_MS = 200

async function main() {
  const ndjsonFile = process.argv[2] ?? DEFAULT_NDJSON
  const envelopes: Envelope[] = fs
    .readFileSync(ndjsonFile, { encoding: 'utf-8' })
    .trim()
    .split('\n')
    .map((line) => JSON.parse(line) as Envelope)

  const printer = new ProgressBarPrinter({
    options: {
      interference: {
        mode: 'suppress',
        streams: [process.stdout, process.stderr],
      },
    },
  })

  for (const envelope of envelopes) {
    if (envelope.testStepFinished) {
      console.log('interference on stdout')
      console.error('interference on stderr')
    }
    printer.update(envelope)
    await setTimeout(DELAY_MS)
  }
}

main().catch((err) => {
  process.stderr.write(`harness failed: ${err}\n`)
  process.exit(1)
})
