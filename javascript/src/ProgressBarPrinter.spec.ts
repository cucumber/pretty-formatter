import fs from 'node:fs'
import * as path from 'node:path'
import { setImmediate } from 'node:timers/promises'

import { Envelope } from '@cucumber/messages'
import { expect } from 'chai'
import { globbySync } from 'globby'

import { makeFakeStream } from '../test/makeFakeStream'
import { ProgressBarPrinter } from './ProgressBarPrinter'
import { indent } from './utils'

const updateExpectedFiles = process.env.UPDATE_EXPECTED_FILES === 'true'

describe('ProgressBarPrinter', () => {
  const ndjsonFiles = globbySync(`*.ndjson`, {
    cwd: path.join(__dirname, '..', '..', 'testdata', 'src'),
    absolute: true,
  })

  for (const ndjsonFile of ndjsonFiles) {
    const [suiteName] = path.basename(ndjsonFile).split('.')

    it(suiteName, async () => {
      const stream = makeFakeStream()
      const printer = new ProgressBarPrinter({
        stream,
      })

      const ndjsonContent = fs.readFileSync(ndjsonFile, { encoding: 'utf-8' })
      const envelopes: Envelope[] = ndjsonContent
        .trim()
        .split('\n')
        .map((line) => JSON.parse(line) as Envelope)

      let previousContent = ''
      const changes: Array<[Envelope, string]> = []
      for (const envelope of envelopes) {
        printer.update(envelope)
        await setImmediate()

        if (stream.content !== previousContent) {
          changes.push([envelope, stream.content])
          previousContent = stream.content
        }
      }

      const capturedLog = changes
        .map(([envelope, content]) => `[${Object.keys(envelope)}]\n${indent(content, 2)}`)
        .join('\n')

      const expectedPath = ndjsonFile.replace('.ndjson', '.cucumber.progressbar.log')
      if (updateExpectedFiles) {
        fs.writeFileSync(expectedPath, capturedLog, { encoding: 'utf-8' })
      }
      const expectedOutput = fs.readFileSync(expectedPath, { encoding: 'utf-8' })

      expect(capturedLog).to.eq(expectedOutput)
    })
  }

  it('should handle messages in quick succession', async function () {
    if (updateExpectedFiles) {
      this.skip()
    }

    const stream = makeFakeStream()
    const printer = new ProgressBarPrinter({
      stream,
    })

    const ndjsonFile = path.join(__dirname, '..', '..', 'testdata', 'src', 'all-statuses.ndjson')
    const ndjsonContent = fs.readFileSync(ndjsonFile, { encoding: 'utf-8' })
    const envelopes: Envelope[] = ndjsonContent
      .trim()
      .split('\n')
      .map((line) => JSON.parse(line) as Envelope)

    for (const envelope of envelopes) {
      printer.update(envelope)
    }
    await setImmediate()

    const capturedLog = stream.content
    const fullOutputPath = ndjsonFile.replace('.ndjson', '.cucumber.progressbar.log')
    const fullOutputContent = fs.readFileSync(fullOutputPath, { encoding: 'utf-8' })
    expect(fullOutputContent).to.include(`[testRunFinished]\n${indent(capturedLog, 2)}`)
  })
})
