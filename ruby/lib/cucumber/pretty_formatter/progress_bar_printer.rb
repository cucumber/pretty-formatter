# frozen_string_literal: true

module Cucumber
  module PrettyFormatter
    PROGRESS_BAR_DEFAULT_OPTIONS = {
      include_attachments: true,
      summarise: false,
      theme: CUCUMBER_THEME,
      format_code: ->(snippet) { snippet }
    }.freeze

    ProgressBarState = Struct.new(
      :phase, :finished_scenarios, :finished_steps, :running_scenarios, :total_scenarios, :total_steps,
      keyword_init: true
    ) do
      def self.empty
        new(phase: :preparing, finished_scenarios: 0, finished_steps: 0, running_scenarios: 0, total_scenarios: 0,
            total_steps: 0)
      end
    end

    class ProgressBarPrinter < Printer
      NON_REPORTABLE_STATUSES = %w[PASSED SKIPPED].freeze

      def initialize(stream: $stdout, **options)
        super(stream: stream, **PROGRESS_BAR_DEFAULT_OPTIONS.merge(options))
        @state = ProgressBarState.empty
        @renderer = ProgressBarRenderer.new(
          stream: stream,
          options: self.options,
          summary_report: method(:summary_report)
        )
      end

      def update(envelope)
        query.update(envelope)
        return nil if state.phase == :done

        handle_all(envelope)
        nil
      end

      private

      attr_reader :state, :renderer

      def handle_all(envelope)
        handle_undefined_parameter_type(envelope_value(envelope, :undefined_parameter_type))
        rerender(initial: true) if envelope_value(envelope, :test_run_started)
        handle_test_case(envelope_value(envelope, :test_case))
        handle_test_run_hook_finished(envelope_value(envelope, :test_run_hook_finished))
        handle_test_case_started if envelope_value(envelope, :test_case_started)
        handle_test_step_finished if envelope_value(envelope, :test_step_finished)
        handle_test_case_finished(envelope_value(envelope, :test_case_finished))
        handle_test_run_finished(envelope_value(envelope, :test_run_finished))
      end

      def handle_undefined_parameter_type(undefined_parameter_type)
        undefined_parameter_type && renderer.add_problem(
          'PARAMETER_TYPE', Formatting.format_undefined_parameter_type(undefined_parameter_type)
        )
      end

      def handle_test_case(test_case)
        return unless test_case

        state.total_scenarios += 1
        state.total_steps += test_case.test_steps.length
        rerender
      end

      def handle_test_run_hook_finished(test_run_hook_finished)
        return unless test_run_hook_finished
        return if NON_REPORTABLE_STATUSES.include?(test_run_hook_finished.result.status)

        renderer.add_problem('GLOBAL_HOOK', summary_report.global_hook_summary(test_run_hook_finished))
        rerender
      end

      def handle_test_case_started
        state.running_scenarios += 1
        state.phase = :running
        rerender
      end

      def handle_test_step_finished
        state.finished_steps += 1
        rerender
      end

      def handle_test_case_finished(test_case_finished)
        return unless test_case_finished

        state.running_scenarios -= 1
        state.finished_scenarios += 1
        # rubocop:disable Layout/LineLength -- ternary keeps this method and class inside Metrics thresholds.
        test_case_finished.will_be_retried ? discount_retry(test_case_finished) : record_scenario_problem(test_case_finished)
        # rubocop:enable Layout/LineLength
        rerender
      end

      def discount_retry(test_case_finished)
        test_case = query.find_test_case_by(test_case_finished)
        return unless test_case

        state.finished_scenarios -= 1
        state.finished_steps -= test_case.test_steps.length
      end

      def record_scenario_problem(test_case_finished)
        result = query.find_most_severe_test_step_result_by(test_case_finished)
        return if !result || NON_REPORTABLE_STATUSES.include?(result.status)

        renderer.add_problem('TEST_CASE', summary_report.scenario_summary(test_case_finished))
      end

      def handle_test_run_finished(test_run_finished)
        return unless test_run_finished

        state.phase = :done
        record_test_run_problem(test_run_finished.exception) if test_run_finished.exception
        rerender
      end

      def record_test_run_problem(exception)
        error = exception.stack_trace || exception.message || 'Unknown error'
        formatted_error = Formatting.indent(Formatting.format_error(error, 'FAILED', options.fetch(:theme), stream), 4)
        renderer.add_problem('TEST_RUN', "\n#{formatted_error}")
      end

      def rerender(initial: false)
        renderer.render_progress(state, initial: initial)
      end

      def summary_report(summary_stream = stream)
        SummaryReport.new(query: query, stream: summary_stream, **options)
      end

      def envelope_value(envelope, name)
        envelope.public_send(name) if envelope.respond_to?(name)
      end
    end
  end
end
