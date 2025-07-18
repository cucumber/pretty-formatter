<h1 align="center">
  <img alt="" width="75" src="https://github.com/cucumber.png"/>
  <br>
  pretty-formatter
</h1>
<p align="center">
  <b>Writes a rich report of the scenario and example execution as it happens</b>
</p>

<p align="center">
  <a href="https://www.npmjs.com/package/@cucumber/pretty-formatter" style="text-decoration: none"><img src="https://img.shields.io/npm/v/@cucumber/pretty-formatter?style=flat&color=dark-green" alt="Latest version on npm"></a>
  <a href="https://search.maven.org/search?q=g:io.cucumber%20AND%20a:pretty-formatter" style="text-decoration: none"><img src="https://img.shields.io/maven-central/v/io.cucumber/pretty-formatter.svg?label=Maven%20Central" alt="Latest version on Maven Central"></a>
</p>

<p align="center">
  <a href="https://github.com/cucumber/pretty-formatter/actions" style="text-decoration: none"><img src="https://github.com/cucumber/pretty-formatter/actions/workflows/test-javascript.yaml/badge.svg" alt="Build status"></a>
  <a href="https://github.com/cucumber/pretty-formatter/actions" style="text-decoration: none"><img src="https://github.com/cucumber/pretty-formatter/actions/workflows/test-java.yml/badge.svg" alt="Build status"></a>
</p>


Pretty Formatter
================

Writes a rich report of the scenario and example execution as it happens. Useful when running Cucumber from the terminal.

![Example output of the pretty formatting, showing the different colors used](https://github.com/user-attachments/assets/feed2857-b8cb-4663-9a5a-57044cfa5356)

## Test outcome coloring

Each step is colored according to the outcome. When the `cucumber` theme is in
use the following colors are used.

| Cucumber Outcome | Color  | Symbol |
|------------------|--------|--------|
| UNKNOWN          | n/a    | n/a    |
| PASSED           | Green  | ✔      |
| SKIPPED          | Cyan   | ↷      |
| PENDING          | Yellow | ■      |
| UNDEFINED        | Yellow | ■      |
| AMBIGUOUS        | Red    | ✘      |
| FAILED           | Red    | ✘      |

## Usage and installation

* [Java](./java/README.md)
* [Javascript](./javascript/README.md)

## Contributing

Each language implementation validates itself against the examples in the
`testdata` folder. See the [testdata/README.md](testdata/README.md) for more
information.
