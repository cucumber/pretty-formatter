# frozen_string_literal: true

# rubocop:disable Metrics/ClassLength, Metrics/AbcSize, Metrics/CyclomaticComplexity
# rubocop:disable Metrics/MethodLength, Metrics/PerceivedComplexity
module Cucumber
  module PrettyFormatter
    class SummaryReport
      NON_REPORTABLE_STATUSES = %w[PASSED SKIPPED].freeze
      GHERKIN_INDENT_LENGTH = 2
      STEP_ARGUMENT_INDENT_LENGTH = 2
      ERROR_INDENT_LENGTH = 4
      ATTACHMENT_INDENT_LENGTH = 4

      attr_reader :query, :stream, :options

      def initialize(query:, stream: $stdout, **options)
        @query = query
        @stream = stream
        @options = options
        configure_stream_color
      end

      def stats
        lines = []

        test_run_finished = query.find_test_run_finished
        if test_run_finished&.exception
          lines << ''
          lines << Formatting.format_counts(
            ['test run', 'test runs'],
            { 'FAILED' => 1 },
            options.fetch(:theme),
            stream
          )
        end

        test_run_hook_finished = query.find_all_test_run_hook_finished
        if test_run_hook_finished.any?
          hook_counts = counts_by_status(test_run_hook_finished.map { |hook| hook.result.status })
          lines << Formatting.format_counts(%w[hook hooks], hook_counts, options.fetch(:theme), stream)
        end

        scenario_statuses = query.find_all_test_case_finished.map do |test_case_finished|
          query.find_most_severe_test_step_result_by(test_case_finished)&.status || 'PASSED'
        end
        lines << Formatting.format_counts(
          %w[scenario scenarios],
          counts_by_status(scenario_statuses),
          options.fetch(:theme),
          stream
        )

        step_statuses = query.find_all_test_case_finished.flat_map do |test_case_finished|
          query.find_test_steps_finished_by(test_case_finished).map { |finished| finished.test_step_result.status }
        end
        lines << Formatting.format_counts(
          %w[step steps],
          counts_by_status(step_statuses),
          options.fetch(:theme),
          stream
        )

        test_run_duration = query.find_test_run_duration
        if test_run_duration
          execution_durations = query.find_all_test_run_hook_finished.map { |hook| hook.result.duration } +
                                query.find_all_test_step_finished.map { |step| step.test_step_result.duration }
          lines << Formatting.format_durations(test_run_duration, execution_durations)
        end

        lines.join("\n")
      end

      def scenario_summary(test_case_finished)
        test_case_started = query.find_test_case_started_by(test_case_finished)
        pickle = query.find_pickle_by(test_case_finished)
        location = query.find_location_of(pickle)
        all_steps = query.find_test_step_finished_and_test_step_by(test_case_started)
        pertinent_steps = find_pertinent_steps(all_steps)

        formatted_location = format_pickle_location(pickle, location)
        formatted_attempt = if test_case_started.attempt.positive?
                              ", after #{test_case_started.attempt + 1} attempts"
                            else
                              ''
                            end
        lines = ["#{pickle.name}#{formatted_attempt} #{formatted_location}"]
        pertinent_steps.each do |test_step_finished, test_step|
          lines.concat(format_step(test_step_finished, test_step))
        end
        lines.join("\n")
      end

      def global_hook_summary(test_run_hook_finished)
        hook = query.find_hook_by(test_run_hook_finished)
        status = test_run_hook_finished.result.status
        lines = [join(
          Formatting.format_hook_title(hook, status, {}, stream),
          hook&.source_reference ? Formatting.format_source_reference(hook.source_reference, options.fetch(:theme),
                                                                      stream) : nil
        )]
        error = test_run_hook_finished.result.exception&.stack_trace ||
                test_run_hook_finished.result.exception&.message ||
                test_run_hook_finished.result.message
        if error
          lines << Formatting.indent(Formatting.format_error(error, status, options.fetch(:theme), stream),
                                     GHERKIN_INDENT_LENGTH)
        end
        lines.join("\n")
      end

      def all_suggestions
        query.find_all_test_case_finished
             .filter_map { |test_case_finished| query.find_pickle_by(test_case_finished) }
             .flat_map { |pickle| query.find_suggestions_by(pickle) }
      end

      def snippets(suggestions)
        seen = {}
        snippets = suggestions.flat_map(&:snippets).reject do |snippet|
          seen.key?(snippet.code).tap { seen[snippet.code] = true }
        end
        lines = ['', 'You can implement missing steps with the snippets below:', '']
        snippets.each do |snippet|
          formatter = options.fetch(:format_code)
          formatted = formatter.arity == 1 ? formatter.call(snippet) : formatter.call(snippet, stream)
          lines << (formatted.respond_to?(:code) ? formatted.code : formatted)
          lines << ''
        end
        "#{lines.join("\n")}\n"
      end

      private

      def find_pertinent_steps(steps_with_finished)
        result = []
        found_first_non_passed = false
        steps_with_finished.each do |test_step_finished, test_step|
          status = test_step_finished.test_step_result.status
          if found_first_non_passed
            result << [test_step_finished, test_step] unless NON_REPORTABLE_STATUSES.include?(status)
          elsif status != 'PASSED'
            result << [test_step_finished, test_step]
            found_first_non_passed = true
          end
        end
        result
      end

      def format_step(test_step_finished, test_step)
        lines = []
        status = test_step_finished.test_step_result.status

        if test_step.pickle_step_id
          lines.concat(format_pickle_step(test_step, status))
        elsif test_step.hook_id
          lines << Formatting.indent(format_hook_step(test_step, status), GHERKIN_INDENT_LENGTH)
        end

        error = Formatting.extract_reportable_message(test_step_finished.test_step_result)
        if error
          lines << Formatting.indent(Formatting.format_error(error, status, options.fetch(:theme), stream),
                                     GHERKIN_INDENT_LENGTH + ERROR_INDENT_LENGTH)
        end

        if options.fetch(:include_attachments)
          query.find_attachments_by(test_step_finished).each do |attachment|
            lines << ''
            lines << Formatting.indent(Formatting.format_attachment(attachment, options.fetch(:theme), stream),
                                       GHERKIN_INDENT_LENGTH + ATTACHMENT_INDENT_LENGTH)
          end
        end

        lines
      end

      def format_pickle_step(test_step, status)
        pickle_step = query.find_pickle_step_by(test_step)
        step = query.find_step_by(pickle_step)
        step_definition = query.find_unambiguous_step_definition_by(test_step)
        title = join(
          Formatting.format_step_title(test_step, pickle_step, step, status, false, options.fetch(:theme), stream),
          step_definition&.source_reference ? Formatting.format_source_reference(step_definition.source_reference,
                                                                                 options.fetch(:theme), stream) : nil
        )
        lines = [Formatting.indent(title, GHERKIN_INDENT_LENGTH)]
        if pickle_step.argument
          lines << Formatting.indent(
            Formatting.format_step_argument(pickle_step.argument, options.fetch(:theme), stream),
            GHERKIN_INDENT_LENGTH + STEP_ARGUMENT_INDENT_LENGTH
          )
        end
        if status == 'AMBIGUOUS'
          lines << Formatting.indent(
            Formatting.format_ambiguous_step(
              query.find_step_definitions_by(test_step), options.fetch(:theme), stream, fallback_bullet: '-'
            ),
            GHERKIN_INDENT_LENGTH + ERROR_INDENT_LENGTH
          )
        end
        lines
      end

      def format_hook_step(test_step, status)
        hook = query.find_hook_by(test_step)
        join(
          Formatting.format_hook_title(hook, status, options.fetch(:theme), stream),
          hook&.source_reference ? Formatting.format_source_reference(hook.source_reference, options.fetch(:theme),
                                                                      stream) : nil
        )
      end

      def format_pickle_location(pickle, location)
        return '' unless location

        TextBuilder.new(stream)
                   .append("# #{pickle.uri}:#{location.line}", options.fetch(:theme).fetch(:location, nil))
                   .build
      end

      def counts_by_status(statuses)
        statuses.tally
      end

      def join(*parts)
        parts.compact.reject(&:empty?).join(' ')
      end

      def configure_stream_color
        return unless options.key?(:color)

        stream.instance_variable_set(TextBuilder::ANSI_COLOR_ENABLED_IVAR, options.fetch(:color))
      end
    end
  end
end
# rubocop:enable Metrics/ClassLength, Metrics/AbcSize, Metrics/CyclomaticComplexity
# rubocop:enable Metrics/MethodLength, Metrics/PerceivedComplexity
