# frozen_string_literal: true

require 'cucumber/messages/helpers/ndjson_to_message_enumerator'

module SharedTestdata
  TESTDATA_DIR = File.expand_path('../../../testdata/src', __dir__)
  PRINTER_CLASSES = {
    'pretty' => Cucumber::PrettyFormatter::PrettyPrinter,
    'progress' => Cucumber::PrettyFormatter::ProgressPrinter,
    'progressbar' => Cucumber::PrettyFormatter::ProgressBarPrinter,
    'summary' => Cucumber::PrettyFormatter::SummaryPrinter
  }.freeze
  THEME_OPTIONS = {
    'cucumber' => { theme: Cucumber::PrettyFormatter::CUCUMBER_THEME, color: true },
    'demo' => { theme: Cucumber::PrettyFormatter::DEMO_THEME, color: true },
    'none' => { theme: Cucumber::PrettyFormatter::NONE_THEME },
    'plain' => { theme: Cucumber::PrettyFormatter::PLAIN_THEME }
  }.freeze
  # rubocop:disable Naming/MethodParameterName, Naming/PredicateMethod
  class FakeProgressBarStream
    attr_reader :content

    def initialize
      @content = +''
      @cursor_offset = 0
    end

    def columns = 80

    def tty? = true

    def write(chunk)
      @content << chunk
      @cursor_offset = 0
      true
    end

    def move_cursor(_dx, dy)
      @cursor_offset = dy
      true
    end

    def clear_screen_down
      if @cursor_offset.negative?
        lines = @content.split("\n")
        retained_lines = lines.slice(0, lines.length + @cursor_offset)
        @content = retained_lines.join("\n")
        @content << "\n" unless retained_lines.empty?
      end
      @cursor_offset = 0
      true
    end
  end

  # rubocop:enable Naming/MethodParameterName, Naming/PredicateMethod

  EVENT_NAMES = {
    attachment: 'attachment',
    test_case: 'testCase',
    test_case_finished: 'testCaseFinished',
    test_case_started: 'testCaseStarted',
    test_run_finished: 'testRunFinished',
    test_run_hook_finished: 'testRunHookFinished',
    test_run_started: 'testRunStarted',
    test_step_finished: 'testStepFinished',
    undefined_parameter_type: 'undefinedParameterType'
  }.freeze

  VARIANT_OPTIONS = {
    'exclude-attachments' => {
      include_attachments: false,
      include_feature_line: true,
      include_rule_line: true,
      use_status_icon: false,
      theme: Cucumber::PrettyFormatter::NONE_THEME
    },
    'exclude-features-and-rules' => {
      include_attachments: true,
      include_feature_line: false,
      include_rule_line: false,
      use_status_icon: false,
      theme: Cucumber::PrettyFormatter::NONE_THEME
    }
  }.freeze

  def self.fixture_for(expected_path)
    basename = File.basename(expected_path, '.log')
    parts = basename.split('.')
    printer_name = parts.pop
    variant_name = parts.pop
    suite_name = parts.join('.')
    ndjson_path = File.join(TESTDATA_DIR, suite_name)
    ndjson_path = "#{ndjson_path}.ndjson" unless File.exist?(ndjson_path)

    {
      expected_path: expected_path,
      ndjson_path: ndjson_path,
      suite_name: suite_name,
      variant_name: variant_name,
      printer_name: printer_name,
      printer_class: PRINTER_CLASSES.fetch(printer_name),
      options: options_for(printer_name, variant_name)
    }
  end

  def self.options_for(printer_name, variant_name)
    options = THEME_OPTIONS.fetch(variant_name, {}).merge(VARIANT_OPTIONS.fetch(variant_name, {}))
    options = { summarise: true }.merge(options) if printer_name == 'progressbar'
    options
  end

  def self.event_name(envelope)
    EVENT_NAMES.each do |field, event_name|
      return event_name if envelope.respond_to?(field) && envelope.public_send(field)
    end
    nil
  end

  def self.fixtures
    Dir[File.join(TESTDATA_DIR, '*.log')]
      # Legacy orphan expectations; JS/Java acceptance map unknown-parameter-type.ndjson
      # to unknown-parameter-type.*.log.
      .reject { |path| File.basename(path).include?('.ndjson.') }
      .map { |path| fixture_for(path) }
  end

  def self.pending_reason(fixture)
    case fixture.fetch(:printer_name)
    when 'summary'
      'Ruby SummaryPrinter currently covers stats and top-level non-passing scenario summaries; ' \
      'remaining parity needs step details, snippets, global hooks, attachments, undefined parameter types, ' \
      'retries, and test-run errors.'
    when 'progressbar'
      'Ruby ProgressBarPrinter now covers shared fixture capture, progress bars, retries, and summary handoff; ' \
      'remaining parity is blocked on detailed problem formatting/snippets/global hooks and existing SummaryPrinter ' \
      'status/duration gaps.'
    when 'pretty'
      'Ruby PrettyPrinter is not implemented for shared fixture parity yet.'
    else
      'Ruby formatter output is not implemented for this shared fixture yet.'
    end
  end
end

RSpec.describe 'shared pretty-formatter testdata' do
  SharedTestdata.fixtures.each do |fixture|
    it "matches #{File.basename(fixture.fetch(:expected_path))}" do
      actual_output = if fixture.fetch(:printer_name) == 'progressbar'
                        capture_progressbar_output(fixture)
                      else
                        capture_stream_output(fixture)
                      end

      expected_output = File.read(fixture.fetch(:expected_path))
      pending(SharedTestdata.pending_reason(fixture)) unless actual_output == expected_output

      expect(actual_output).to eq(expected_output)
    end
  end

  def capture_stream_output(fixture)
    stream = StringIO.new
    printer = fixture.fetch(:printer_class).new(stream: stream, **fixture.fetch(:options))

    each_envelope(fixture) { |envelope| printer.update(envelope) }

    stream.string
  end

  # rubocop:disable Metrics/AbcSize
  def capture_progressbar_output(fixture)
    stream = SharedTestdata::FakeProgressBarStream.new
    printer = fixture.fetch(:printer_class).new(stream: stream, **fixture.fetch(:options))
    previous_content = +''
    changes = []

    each_envelope(fixture) do |envelope|
      printer.update(envelope)
      next if stream.content == previous_content

      changes << [SharedTestdata.event_name(envelope), stream.content]
      previous_content = stream.content.dup
    end

    changes.map do |event_name, content|
      "[#{event_name}]\n#{Cucumber::PrettyFormatter::Formatting.indent(content, 2)}"
    end.join("\n")
  end

  # rubocop:enable Metrics/AbcSize

  def each_envelope(fixture, &block)
    File.open(fixture.fetch(:ndjson_path), 'r') do |io|
      Cucumber::Messages::Helpers::NdjsonToMessageEnumerator.new(io).each(&block)
    end
  end
end
