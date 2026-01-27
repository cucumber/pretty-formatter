# Contributing

## Requirements

You'll need Node.js installed. Install dependencies with:

```shell
npm install
```

## Dependencies

Dependencies are kept to a minimum.

We use the `@cucumber/query` library to populate a `Query` object in each printer that can be interrogated to get the required data from the test run. 

Terminal styling is done with [`styleText`](https://nodejs.org/api/util.html#utilstyletextformat-text-options) from `node:util`, using the `stream` option for correct feature detection. We avoid third-party terminal/colour libraries in favour of this built-in. Our `TextBuilder` class wraps `styleText` to provide a convenient way to accumulate styled text.

## Structure

Aside from the printer classes themselves, there are several layers of shared helper functions:

- `src/formatting` - functions that take some specific data and return a formatted string based on the theme and stream
- `src/composition` - functions that pull together disparate data with a `Query` object and compose a longer formatted string from several format functions
- `src/queries` - functions that extend `Query` functionality with more specialised queries and don't quite make sense to add to the upstream library

## Testing

The acceptance test suite is shared with the other implementations in this polyglot repo. If you're adding or changing behaviour, and doing it in this implementation first, you can run this to regenerate the shared fixtures:

```shell
npm run update-expected-files
```