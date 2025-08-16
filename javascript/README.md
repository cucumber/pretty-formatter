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
  <a href="https://github.com/cucumber/pretty-formatter/actions" style="text-decoration: none"><img src="https://github.com/cucumber/pretty-formatter/actions/workflows/test-javascript.yaml/badge.svg" alt="Build status"></a>
</p>

![Example output of the pretty formatting, showing the different colors used](https://github.com/user-attachments/assets/feed2857-b8cb-4663-9a5a-57044cfa5356)

## Usage

For usage in `@cucumber/cucumber`, see https://github.com/cucumber/cucumber-js/blob/main/docs/formatters.md#pretty

## Options

- `attachments` - whether to include attachments (defaults to `true`)
- `featuresAndRules` - whether to include headings for Features and Rules (defaults to `true`)
- `theme` - control over the styling of various elements (see below)

## Themes

Here's the schema for a theme:

```ts
interface Theme {
    attachment?: Style
    dataTable?: {
        all?: Style
        border?: Style
        content?: Style
    }
    docString?: {
        all?: Style
        content?: Style
        delimiter?: Style
        mediaType?: Style
    }
    feature?: {
        all?: Style
        keyword?: Style
        name?: Style
    }
    location?: Style
    rule?: {
        all?: Style
        keyword?: Style
        name?: Style
    }
    scenario?: {
        all?: Style
        keyword?: Style
        name?: Style
    }
    status?: {
        all?: Partial<Record<TestStepResultStatus, Style>>
        icon?: Partial<Record<TestStepResultStatus, string>>
    }
    step?: {
        argument?: Style
        keyword?: Style
        text?: Style
    }
    tag?: Style
}

enum TestStepResultStatus {
    UNKNOWN = "UNKNOWN",
    PASSED = "PASSED",
    SKIPPED = "SKIPPED",
    PENDING = "PENDING",
    UNDEFINED = "UNDEFINED",
    AMBIGUOUS = "AMBIGUOUS",
    FAILED = "FAILED"
}
```

`Style` is any [Node.js supported modifier](https://nodejs.org/api/util.html#modifiers) or an array of them.

See the [default theme](./src/theme.ts) for a good example. It's exported as `CUCUMBER_THEME`, so you can clone and extend it if you'd like.