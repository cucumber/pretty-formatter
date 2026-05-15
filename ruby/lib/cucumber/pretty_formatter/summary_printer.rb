# frozen_string_literal: true

module Cucumber
  module PrettyFormatter
    class SummaryPrinter < Printer
      DEFAULT_OPTIONS = {
        include_attachments: true,
        theme: CUCUMBER_THEME,
        format_code: ->(snippet, _stream = nil) { snippet.code }
      }.freeze

      def initialize(stream: $stdout, **options)
        super(stream: stream, **DEFAULT_OPTIONS.merge(options))
      end

      def update(envelope)
        query.update(envelope)
        print_summary if envelope.respond_to?(:test_run_finished) && envelope.test_run_finished
        nil
      end

      def self.summarise(query, stream: $stdout, **options)
        new(stream: stream, **options).tap do |printer|
          printer.instance_variable_set(:@query, query)
          printer.print_summary
        end
        stream
      end

      def print_summary
        problem_printer.print_non_passing_scenarios
        problem_printer.print_unknown_parameter_types
        problem_printer.print_non_passing_global_hooks
        problem_printer.print_non_passing_test_run
        print_stats
        print_snippets
      end

      private

      def print_stats
        println
        println(summary_report.stats)
      end

      def print_snippets
        suggestions = summary_report.all_suggestions
        return if suggestions.empty?

        print(summary_report.snippets(suggestions))
      end

      def problem_printer
        @problem_printer ||= SummaryProblemPrinter.new(
          query: query,
          stream: stream,
          summary_report: summary_report,
          **options
        )
      end

      def summary_report
        @summary_report ||= SummaryReport.new(query: query, stream: stream, **options)
      end

      def print(text)
        stream.write(text)
      end

      def println(text = '')
        stream.write("#{text}\n")
      end
    end
  end
end
