# Cucumber Pretty Formatter for Ruby

`cucumber-pretty-formatter` renders Cucumber Messages to terminal-oriented
output. It is message-driven: each printer consumes `Cucumber::Messages::Envelope`
objects through `#update` and writes formatted text to a stream.

The Ruby package mirrors the JavaScript/Java pretty-formatter behaviour while
using Ruby naming conventions.

## Installation

```ruby
gem 'cucumber-pretty-formatter', '~> 15.0'
```

Ruby 3.2 or newer is required. The gem depends on `cucumber-messages` and
`cucumber-query`.

## Message flow

Each printer owns a `Cucumber::Query::Query` instance. Calling `#update(envelope)`
first updates the query, then renders any output triggered by that envelope.
Feed envelopes in the order Cucumber produced them:

```ruby
require 'cucumber/pretty_formatter'
require 'cucumber/messages/helpers/ndjson_to_message_enumerator'

printer = Cucumber::PrettyFormatter::PrettyPrinter.new(stream: $stdout)

File.open('cucumber-messages.ndjson', 'r') do |io|
  Cucumber::Messages::Helpers::NdjsonToMessageEnumerator.new(io).each do |envelope|
    printer.update(envelope)
  end
end
```

`#update` returns `nil`. Output is written incrementally to the configured
stream.

## Printers

All printers live under `Cucumber::PrettyFormatter` and accept `stream:` plus
printer options as keyword arguments.

### PrettyPrinter

Renders feature, rule, scenario, step, attachment, error, and optional summary
output in the familiar pretty formatter style.

```ruby
stream = StringIO.new
printer = Cucumber::PrettyFormatter::PrettyPrinter.new(
  stream: stream,
  include_attachments: true,
  include_feature_line: true,
  include_rule_line: true,
  use_status_icon: true,
  summarise: true,
  theme: Cucumber::PrettyFormatter::CUCUMBER_THEME
)

envelopes.each { |envelope| printer.update(envelope) }
puts stream.string
```

### ProgressPrinter

Writes one progress character for each finished test step or global hook, then a
newline when the test run finishes.

```ruby
printer = Cucumber::PrettyFormatter::ProgressPrinter.new(
  stream: $stdout,
  theme: Cucumber::PrettyFormatter::PLAIN_THEME
)
```

### ProgressBarPrinter

Renders scenario and step progress bars and reports problems as they are
discovered. For terminal streams that support it, the printer calls
`move_cursor(dx, dy)`, `clear_screen_down`, and `columns` to redraw in place.
Plain streams still work; they will receive each rendered update as text.

```ruby
printer = Cucumber::PrettyFormatter::ProgressBarPrinter.new(
  stream: $stdout,
  summarise: true,
  theme: Cucumber::PrettyFormatter::CUCUMBER_THEME
)
```

### SummaryPrinter

Prints final summaries when it receives `test_run_finished`. It can also render
a summary for an existing query, which is how `PrettyPrinter` and
`ProgressBarPrinter` delegate summary output.

```ruby
printer = Cucumber::PrettyFormatter::SummaryPrinter.new(
  stream: $stdout,
  include_attachments: true
)

envelopes.each { |envelope| printer.update(envelope) }

# Or render from a query already populated by another printer:
Cucumber::PrettyFormatter::SummaryPrinter.summarise(existing_query, stream: $stdout)
```

## Constructor options

Options are Ruby keyword arguments. Unknown options are currently stored on the
printer and ignored unless a printer reads them.

| Option | Printers | Default | Meaning |
| --- | --- | --- | --- |
| `stream:` | all | `$stdout` | Object receiving `write(string)` calls. |
| `theme:` | all | `CUCUMBER_THEME` | Theme hash controlling ANSI styles, status icons, progress characters, and bullets. |
| `color:` | all | stream-aware | Override ANSI colour output. `true` forces ANSI, `false` suppresses ANSI; omitted means colour only for TTY streams and never when `NO_COLOR` is set. |
| `include_attachments:` | pretty, progress, progressbar, summary | `true` | Include attachment output where the printer reports details. |
| `include_feature_line:` | pretty | `true` | Print feature headings before scenarios. |
| `include_rule_line:` | pretty | `true` | Print rule headings before scenarios. |
| `use_status_icon:` | pretty | `true` | Prefix step lines with status icons when the theme defines them. |
| `summarise:` | pretty, progress, progressbar | `false` | Include/delegate final summary output when supported by the printer. |
| `format_code:` | pretty, progressbar, summary | printer-specific lambda | Format snippet code before printing it. |

`format_code` receives a snippet-like object for summary output. The default
summary formatter returns `snippet.code`; the pretty/progressbar defaults return
the value unchanged until they delegate to summary output.

## Themes and ANSI handling

Built-in themes:

- `Cucumber::PrettyFormatter::CUCUMBER_THEME` - default coloured Cucumber style.
- `Cucumber::PrettyFormatter::DEMO_THEME` - high-visibility/demo styling used by
  shared fixtures.
- `Cucumber::PrettyFormatter::PLAIN_THEME` - no ANSI colours, but keeps status
  icons/progress characters and `-` bullets.
- `Cucumber::PrettyFormatter::NONE_THEME` - minimal output with no colours,
  status icons, or progress characters unless explicitly present in the theme.

Themes are nested hashes. Styles are symbolic ANSI styles such as `:green`,
`:red`, `:bold`, `:italic`, or arrays of styles. You can pass a custom theme
that overrides only the sections you need:

```ruby
plain_with_dots = Cucumber::PrettyFormatter::PLAIN_THEME.merge(
  status: Cucumber::PrettyFormatter::PLAIN_THEME.fetch(:status).merge(
    progress: { 'PASSED' => '.', 'FAILED' => 'F' }
  )
)

printer = Cucumber::PrettyFormatter::ProgressPrinter.new(
  stream: $stdout,
  theme: plain_with_dots
)
```

ANSI codes are emitted by the formatter's `TextBuilder` according to the theme
when the output stream reports `tty? == true`. Non-TTY streams and environments
with `NO_COLOR` set suppress ANSI by default. Pass `color: true` to force ANSI
(for example in golden fixture tests), or `color: false` to suppress it even for
a TTY stream. You can also choose `PLAIN_THEME` or `NONE_THEME` for deterministic
non-colour output.

## Local development dependencies

Use released gems by default. To test against local checkouts without editing
this repository, set environment variables before running Bundler:

```sh
cd repos/pretty-formatter/ruby
CUCUMBER_MESSAGES_RUBY_PATH=../../messages/ruby \
CUCUMBER_QUERY_RUBY_PATH=../../query/ruby \
bundle install
```

You can also test against an unreleased query branch/ref:

```sh
CUCUMBER_QUERY_RUBY_REF=ruby-implementation bundle install
```

Run the Ruby checks from this directory:

```sh
bundle exec rake
```

The acceptance specs auto-discover shared expected output fixtures from
`../testdata/src` and run the matching Ruby printer. Fixtures that are not yet at
parity are marked pending in the spec while still executing their assertions.

## Ruby release sequencing

The Ruby `cucumber-pretty-formatter` gem depends on the released
`cucumber-query` Ruby gem (`>= 15.0.0`, `< 16.0.0`). Merge and publish the Ruby
`cucumber-query` gem before merging or releasing the Ruby
`cucumber-pretty-formatter` gem, otherwise RubyGems installs cannot resolve the
formatter dependency.

While the query and formatter Ruby changes are being developed together, CI may
use `CUCUMBER_QUERY_RUBY_PATH` or `CUCUMBER_QUERY_RUBY_REF` to test against an
unreleased query checkout or branch. Remove or gate any temporary branch ref
before the formatter changes land on `main`; the formatter release should depend
on the published query gem, not a development branch.

## Performance and thread-safety notes

Each printer keeps a full `Cucumber::Query::Query` index for the message stream
so it can correlate runtime events with source, pickle, hook, attachment, and
snippet messages. Memory use grows with the size of the test run.

Printer instances are mutable and are not designed for concurrent `#update`
calls. Use one printer per message stream, or synchronize externally if a stream
is shared across threads.

## Troubleshooting and current limitations

- Feed complete, ordered Cucumber Messages streams. Missing definition messages
  lead to missing locations, step text, hook names, or summaries.
- If output includes ANSI escape codes, use `PLAIN_THEME` or `NONE_THEME` in
  tests or non-terminal consumers.
- `ProgressBarPrinter` redraws in place only when the stream implements terminal
  cursor helpers; otherwise it appends updates.
- The Ruby implementation is developed against shared pretty-formatter fixtures.
  Some fixture parity is still pending; pending specs document the current gaps
  and will fail when output unexpectedly reaches parity.
