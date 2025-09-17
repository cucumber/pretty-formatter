[![Maven Central](https://img.shields.io/maven-central/v/io.cucumber/pretty-formatter.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:io.cucumber%20AND%20a:pretty-formatter)

Pretty Formatter Usage and Installation
=======================================

## Installation

⚠️ This is an internal package; you don't need to install it in order to use the Pretty Formatter.

## Theming

The messages to pretty and summary writers can be configured with a custom theme. For example to remove all colors.

```java
var writer = MessagesToPrettyWriter.builder()
        .theme(Theme.builder()
                .style(FEATURE_KEYWORD, Ansi.with(BOLD), Ansi.with(BOLD_OFF))
                .style(RULE_KEYWORD, Ansi.with(BOLD), Ansi.with(BOLD_OFF))
                .style(SCENARIO_KEYWORD, Ansi.with(BOLD), Ansi.with(BOLD_OFF))
                .style(STEP_ARGUMENT, Ansi.with(BOLD), Ansi.with(BOLD_OFF))
                .style(STEP_KEYWORD, Ansi.with(BOLD), Ansi.with(BOLD_OFF))
                .build())
        .build(System.out);
```

## Parallel Execution

If Cucumber is executing scenarios and examples in parallel their steps will
interleave in the reporting. This is an unavoidable side effect of executing in
parallel. The visual clutter can be somewhat reduced by removing the feature and
rule lines from the output.

```java
var writer = MessagesToPrettyWriter.builder()
        .feature(INCLUDE_FEATURE_LINE, false)
        .feature(INCLUDE_RULE_LINE, false)
        .build(System.out);
```

## Step and scenario locations

The location of steps and scenarios is included comment (following the `#`).
Typically, this is a fully qualified URL, which can clutter up the output. This
url can be made shorter by removing the prefix from it. For example:

```java
var cwdUri = new File("").toURI().toString();
var writer = MessagesToPrettyWriter.builder()
        .removeUriPrefix(cwdUri)
        .build(System.out);
```
