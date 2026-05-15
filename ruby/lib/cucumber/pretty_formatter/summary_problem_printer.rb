# frozen_string_literal: true

module Cucumber
  module PrettyFormatter
    class SummaryProblemPrinter
      NON_REPORTABLE_STATUSES = %w[PASSED SKIPPED].freeze
      GHERKIN_INDENT_LENGTH = 2

      attr_reader :query, :stream, :options, :summary_report

      def initialize(query:, stream:, summary_report:, **options)
        @query = query
        @stream = stream
        @summary_report = summary_report
        @options = options
      end

      def print_non_passing_scenarios
        non_passing_scenarios.each do |status, scenarios|
          next if scenarios.empty?

          println
          println(format_for_status(status, "#{format_status_name(status)} scenarios:"))
          scenarios.each_with_index do |test_case_finished, index|
            println(indent_numbered(summary_report.scenario_summary(test_case_finished), 2, index + 1))
          end
        end
      end

      def print_unknown_parameter_types
        unknown_parameter_types = query.find_all_undefined_parameter_types
        return if unknown_parameter_types.empty?

        println
        println(format_for_status('UNDEFINED', 'These parameters are missing a parameter type definition:'))
        unknown_parameter_types.each_with_index do |undefined_parameter_type, index|
          println(indent_numbered(format_undefined_parameter_type(undefined_parameter_type), GHERKIN_INDENT_LENGTH,
                                  index + 1))
        end
      end

      def print_non_passing_global_hooks
        failed_hooks = query.find_all_test_run_hook_finished.select { |hook| hook.result.status == 'FAILED' }
        return if failed_hooks.empty?

        println
        println(format_for_status('FAILED', 'Failed hooks:'))
        failed_hooks.each_with_index do |test_run_hook_finished, index|
          println(indent_numbered(summary_report.global_hook_summary(test_run_hook_finished), GHERKIN_INDENT_LENGTH,
                                  index + 1))
        end
      end

      def print_non_passing_test_run
        test_run_finished = query.find_test_run_finished
        return unless test_run_finished&.exception

        println(format_for_status('FAILED', 'Failed test run:'))
        error = test_run_finished.exception.stack_trace || test_run_finished.exception.message
        print(Formatting.indent(Formatting.format_error(error, 'FAILED', options.fetch(:theme), stream), 7)) if error
      end

      private

      def non_passing_scenarios
        grouped = Hash.new { |hash, key| hash[key] = [] }
        query.find_all_test_case_finished.each_with_object(grouped) do |test_case_finished, result|
          status = query.find_most_severe_test_step_result_by(test_case_finished)&.status
          result[status] << test_case_finished if reportable_status?(status)
        end
        Formatting::ORDERED_STATUSES.map { |status| [status, grouped.fetch(status, [])] }
      end

      def reportable_status?(status)
        status && !NON_REPORTABLE_STATUSES.include?(status)
      end

      def format_undefined_parameter_type(undefined_parameter_type)
        Formatting.format_undefined_parameter_type(undefined_parameter_type)
      end

      def format_for_status(status, text)
        TextBuilder.new(stream).append(text, options.fetch(:theme).dig(:status, :all, status)).build
      end

      def format_status_name(status)
        status[0] + status[1..].downcase
      end

      def indent_numbered(text, by, number)
        baseline_indent = ' ' * by
        text.split("\n").each_with_index.map do |line, index|
          next '' if line.empty?

          index.zero? ? "#{baseline_indent}#{number}) #{line}" : "#{baseline_indent}   #{line}"
        end.join("\n")
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
