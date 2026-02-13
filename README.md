<h1 align="center">
  <img alt="" width="75" src="https://github.com/cucumber.png"/>
  <br>
  pretty-formatter
</h1>
<p align="center">
  <b>Rich formatting of Cucumber progress and results for the terminal</b>
</p>

<p align="center">
  <a href="https://www.npmjs.com/package/@cucumber/pretty-formatter" style="text-decoration: none"><img src="https://img.shields.io/npm/v/@cucumber/pretty-formatter?style=flat&color=dark-green" alt="Latest version on npm"></a>
  <a href="https://search.maven.org/search?q=g:io.cucumber%20AND%20a:pretty-formatter" style="text-decoration: none"><img src="https://img.shields.io/maven-central/v/io.cucumber/pretty-formatter.svg?label=Maven%20Central" alt="Latest version on Maven Central"></a>
</p>

<p align="center">
  <a href="https://github.com/cucumber/pretty-formatter/actions" style="text-decoration: none"><img src="https://github.com/cucumber/pretty-formatter/actions/workflows/test-javascript.yaml/badge.svg" alt="Build status"></a>
  <a href="https://github.com/cucumber/pretty-formatter/actions" style="text-decoration: none"><img src="https://github.com/cucumber/pretty-formatter/actions/workflows/test-java.yml/badge.svg" alt="Build status"></a>
</p>


This package provides several printers and utilities for rich formatting of Cucumber progress and results in the terminal.

## Pretty

The pretty formatter writes a rich report of the scenario and example execution as it happens. Useful when running Cucumber from the terminal.

![Example output of the pretty formatting, showing the different colors used](./screenshots/all-statuses.cucumber.pretty.png)

## Progress

The progress formatter is minimalist progress indicator that writes a single character status for each test. Useful the test suite gets large. Pairs well with the summary formatter.

![Example output of the progress formatting, showing the different colors used](./screenshots/all-statuses.cucumber.progress.png)

## Summary

The summary formatter writes a rich summary at the end of the test run.

![Example output of the summary formatting, showing the different colors used](./screenshots/all-statuses.cucumber.summary.png)

## Test outcome coloring

Each step is colored according to the outcome. When the `cucumber` theme is in
use the following colors and symbols are used.

| Cucumber Outcome | Color   | Status Symbol | Progress Symbol |
|------------------|---------|---------------|-----------------|
| UNKNOWN          | n/a     | n/a           | n/a             |
| PASSED           | Green   | ✔             | .               |
| SKIPPED          | Yellow  | ↷             | -               |
| PENDING          | Cyan    | ■             | P               |
| UNDEFINED        | Blue    | ■             | U               |
| AMBIGUOUS        | Magenta | ✘             | A               |
| FAILED           | Red     | ✘             | F               |

## Usage and installation

* [Java](./java/README.md)
* [JavaScript](./javascript/README.md)

## Contributing

Each language implementation validates itself against the examples in the
`testdata` folder. See the [testdata/README.md](testdata/README.md) for more
information.
