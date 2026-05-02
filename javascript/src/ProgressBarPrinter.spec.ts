import fs from 'node:fs'
import * as path from 'node:path'
import { setImmediate } from 'node:timers/promises'

import type { Envelope } from '@cucumber/messages'
import { expect } from 'chai'
import { globbySync } from 'globby'

import { makeFakeStream } from '../test/makeFakeStream'
import { ProgressBarPrinter } from './ProgressBarPrinter'
import { indent } from './utils'

const updateExpectedFiles = process.env.UPDATE_EXPECTED_FILES === 'true'

describe('ProgressBarPrinter', () => {
  describe('acceptance', () => {
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
          options: {
            summarise: true,
          },
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
  })

  it('should handle messages in quick succession', async function () {
    if (updateExpectedFiles) {
      this.skip()
    }

    const stream = makeFakeStream()
    const printer = new ProgressBarPrinter({
      stream,
      options: {
        summarise: true,
      },
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

  describe('interference', () => {
    const testRunStarted: Envelope = {
      testRunStarted: { id: '1', timestamp: { seconds: 0, nanos: 0 } },
    }
    const testRunFinished: Envelope = {
      testRunFinished: {
        testRunStartedId: '1',
        success: true,
        timestamp: { seconds: 0, nanos: 0 },
      },
    }

    it('should suppress writes that arent ours', async function () {
      if (updateExpectedFiles) {
        this.skip()
      }

      const stream = makeFakeStream()
      const printer = new ProgressBarPrinter({
        stream,
        options: {
          interference: {
            mode: 'suppress',
            streams: [stream],
          },
          summarise: true,
        },
      })

      const ndjsonFile = path.join(__dirname, '..', '..', 'testdata', 'src', 'all-statuses.ndjson')
      const ndjsonContent = fs.readFileSync(ndjsonFile, { encoding: 'utf-8' })
      const envelopes: Envelope[] = ndjsonContent
        .trim()
        .split('\n')
        .map((line) => JSON.parse(line) as Envelope)

      for (const envelope of envelopes) {
        if (envelope.testStepFinished) {
          stream.write('interference')
        }
        printer.update(envelope)
        await setImmediate()
      }

      const capturedLog = stream.content
      const fullOutputPath = ndjsonFile.replace('.ndjson', '.cucumber.progressbar.log')
      const fullOutputContent = fs.readFileSync(fullOutputPath, { encoding: 'utf-8' })
      expect(fullOutputContent).to.include(`[testRunFinished]\n${indent(capturedLog, 2)}`)
    })

    it('restores the original write after the run finishes', () => {
      const stream = makeFakeStream()
      const printer = new ProgressBarPrinter({
        stream,
        options: { interference: { mode: 'suppress', streams: [stream] } },
      })

      printer.update(testRunStarted)
      printer.update(testRunFinished)

      const before = stream.content
      stream.write('after release')
      expect(stream.content).to.eq(`${before}after release`)
    })

    it('intercepts and releases multiple streams independently', () => {
      const renderTarget = makeFakeStream()
      const stream1 = makeFakeStream()
      const stream2 = makeFakeStream()
      const printer = new ProgressBarPrinter({
        stream: renderTarget,
        options: { interference: { mode: 'suppress', streams: [stream1, stream2] } },
      })

      printer.update(testRunStarted)
      stream1.write('foreign 1')
      stream2.write('foreign 2')
      expect(stream1.content).to.eq('')
      expect(stream2.content).to.eq('')

      printer.update(testRunFinished)
      stream1.write('after 1')
      stream2.write('after 2')
      expect(stream1.content).to.eq('after 1')
      expect(stream2.content).to.eq('after 2')
    })

    it('still invokes the write callback when suppressing', (done) => {
      const stream = makeFakeStream()
      const printer = new ProgressBarPrinter({
        stream,
        options: { interference: { mode: 'suppress', streams: [stream] } },
      })

      printer.update(testRunStarted)
      stream.write('foreign', () => done())
    })

    it('still invokes the write callback when given an encoding', (done) => {
      const stream = makeFakeStream()
      const printer = new ProgressBarPrinter({
        stream,
        options: { interference: { mode: 'suppress', streams: [stream] } },
      })

      printer.update(testRunStarted)
      stream.write('foreign', 'utf8', () => done())
    })
  })
})
