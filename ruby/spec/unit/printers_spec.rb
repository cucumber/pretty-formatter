# frozen_string_literal: true

RSpec.describe Cucumber::PrettyFormatter do
  def tty_stream
    StringIO.new.tap { |stream| allow(stream).to receive(:tty?).and_return(true) }
  end

  def non_tty_stream
    StringIO.new.tap { |stream| allow(stream).to receive(:tty?).and_return(false) }
  end

  describe Cucumber::PrettyFormatter::TextBuilder do
    it 'suppresses ANSI styling for streams that are not TTYs by default' do
      stream = non_tty_stream

      expect(described_class.new(stream).append('ok', :green).build).to eq('ok')
    end

    it 'allows ANSI styling for TTY-like streams by default' do
      stream = tty_stream

      expect(described_class.new(stream).append('ok', :green).build).to eq("\e[32mok\e[39m")
    end

    it 'suppresses ANSI styling when NO_COLOR is present' do
      stream = tty_stream

      with_environment('NO_COLOR' => '1') do
        expect(described_class.new(stream).append('ok', :green).build).to eq('ok')
      end
    end

    it 'supports an explicit ANSI override' do
      stream = non_tty_stream

      expect(described_class.new(stream, color: true).append('ok', :green).build).to eq("\e[32mok\e[39m")
      expect(described_class.new(tty_stream, color: false).append('ok', :green).build).to eq('ok')
    end
  end

  describe Cucumber::PrettyFormatter::Formatting do
    describe '.format_status_character' do
      let(:stream) { tty_stream }

      it 'formats progress characters with the cucumber status colour' do
        expect(described_class.format_status_character('PASSED', Cucumber::PrettyFormatter::CUCUMBER_THEME, stream))
          .to eq("\e[32m.\e[39m")
      end

      it 'formats progress characters without colour in the plain theme' do
        expect(described_class.format_status_character('FAILED', Cucumber::PrettyFormatter::PLAIN_THEME, stream))
          .to eq('F')
      end

      it 'uses a blank character when the theme has no progress mapping' do
        expect(described_class.format_status_character('FAILED', Cucumber::PrettyFormatter::NONE_THEME, stream))
          .to eq(' ')
      end
    end

    describe '.format_counts' do
      let(:stream) { StringIO.new }

      it 'formats totals and status counts in canonical order' do
        expect(described_class.format_counts(%w[scenario scenarios], {
                                               'FAILED' => 1,
                                               'PASSED' => 2
                                             }, Cucumber::PrettyFormatter::PLAIN_THEME, stream))
          .to eq('3 scenarios (2 passed, 1 failed)')
      end
    end

    describe '.format_durations' do
      let(:duration) do
        Struct.new(:seconds, :nanos, keyword_init: true)
      end

      it 'matches shared fixture duration scale and formatting' do
        expect(described_class.format_durations(
                 duration.new(seconds: 0, nanos: 5_000_000),
                 [duration.new(seconds: 0, nanos: 1_000_000)]
               )).to eq('0m 0.5s (0m 0.1s executing your code)')
      end
    end
  end

  describe Cucumber::PrettyFormatter::ProgressPrinter do
    subject(:printer) { described_class.new(stream: stream, theme: theme) }

    let(:stream) { StringIO.new }
    let(:theme) { Cucumber::PrettyFormatter::PLAIN_THEME }

    it 'prints a progress character for finished test steps' do
      printer.update(envelope(test_step_finished: test_step_finished('PASSED')))

      expect(stream.string).to eq('.')
    end

    it 'prints a progress character for finished test run hooks' do
      printer.update(envelope(test_run_hook_finished: test_run_hook_finished('FAILED')))

      expect(stream.string).to eq('F')
    end

    it 'prints a newline when the test run finishes' do
      printer.update(envelope(test_run_finished: Object.new))

      expect(stream.string).to eq("\n")
    end

    it 'can force ANSI output for non-TTY streams' do
      printer = described_class.new(stream: non_tty_stream, theme: Cucumber::PrettyFormatter::CUCUMBER_THEME,
                                    color: true)

      printer.update(envelope(test_step_finished: test_step_finished('PASSED')))

      expect(printer.stream.string).to eq("\e[32m.\e[39m")
    end

    def envelope(test_step_finished: nil, test_run_hook_finished: nil, test_run_finished: nil)
      Cucumber::Messages::Envelope.new(
        test_step_finished: test_step_finished,
        test_run_hook_finished: test_run_hook_finished,
        test_run_finished: test_run_finished
      )
    end

    def test_step_finished(status)
      Cucumber::Messages::TestStepFinished.new(test_step_result: test_step_result(status))
    end

    def test_run_hook_finished(status)
      Cucumber::Messages::TestRunHookFinished.new(result: test_step_result(status))
    end

    def test_step_result(status)
      Cucumber::Messages::TestStepResult.new(status: status)
    end
  end

  described_classes = [
    Cucumber::PrettyFormatter::PrettyPrinter,
    Cucumber::PrettyFormatter::ProgressPrinter,
    Cucumber::PrettyFormatter::ProgressBarPrinter,
    Cucumber::PrettyFormatter::SummaryPrinter
  ]

  described_classes.each do |printer_class|
    describe printer_class do
      subject(:printer) { printer_class.new(stream: stream, include_attachments: true, summarise: false) }

      let(:stream) { StringIO.new }
      let(:envelope) { Cucumber::Messages::Envelope.new }

      it 'accepts message envelopes via #update' do
        expect(printer.query).to be_a(Cucumber::Query::Query)
        expect(printer.update(envelope)).to be_nil
        expect(printer.query.envelopes).to eq([envelope])
        expect(stream.string).to eq('')
      end
    end
  end
end
