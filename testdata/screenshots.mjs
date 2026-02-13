import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import ansiToImage from 'ansi-to-image'

const files = [
  'all-statuses.cucumber.pretty.log',
  'all-statuses.cucumber.progress.log',
  'all-statuses.cucumber.summary.log'
]

const screenshotsDir = join(import.meta.dirname, '..', 'screenshots')

const options = {
  colors: join(import.meta.dirname, 'screenshots.itermcolors'),
  fontFamily: 'JetBrains Mono, ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, Liberation Mono, Courier New, monospace',
  fontSize: 14,
  lineHeight: 21,
  paddingTop: 21,
  paddingLeft: 21,
  paddingBottom: 21,
  paddingRight: 21,
  type: 'png'
}

for (const file of files) {
  const inputPath = join(import.meta.dirname, 'src', file)
  const outputPath = join(screenshotsDir, file.replace('.log', '.png'))
  const ansiText = readFileSync(inputPath, 'utf8').trim()
  await ansiToImage(ansiText, { ...options, filename: outputPath })
  console.log(`Generated ${outputPath}`)
}
