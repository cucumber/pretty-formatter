Pretty Formatter Usage and Installation
=======================================

## Usage in cucumber-js >= 11.1.0

Install the package:

```shell
npm install --save-dev @cucumber/pretty-formatter
```

Specify the formatter to Cucumber:

```shell
cucumber-js --format @cucumber/pretty-formatter
```

### Options

Options are under the `pretty` key.

By default, headings are printed for features and rules. You can disable this:

```shell
cucumber-js --format @cucumber/pretty-formatter --format-options '{"pretty": {"featuresAndRules": false}}'
```

By default, a theme with colors and icons is used. You can provide your own:

```shell
cucumber-js --format @cucumber/pretty-formatter --format-options '{"pretty": {"theme": {...}}}'
```

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

## Usage in cucumber-js < 11.1.0

Use the [1.x.x version](https://www.npmjs.com/package/@cucumber/pretty-formatter/v/1.0.1) of this package if you're running an older version of Cucumber.