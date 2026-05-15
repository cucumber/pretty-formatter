# frozen_string_literal: true

require 'stringio'

module Cucumber
  module PrettyFormatter
    class ProgressBarRenderer
      MAX_BAR_WIDTH = 50
      MIN_LEGEND_WIDTH = 30

      attr_reader :stream, :options, :summary_report, :pending_problems, :printed_problems

      def initialize(stream:, options:, summary_report:)
        @stream = stream
        @options = options
        @summary_report = summary_report
        @pending_problems = []
        @printed_problems = []
      end

      def add_problem(type, details)
        pending_problems << [type, details]
      end

      def render_progress(state, initial: false)
        output = +' '
        output.clear
        output << flush_pending_problems
        output << if state.phase == :done && options.fetch(:summarise)
                    make_summary_block
                  else
                    make_progress_block(state)
                  end
        render(output, initial)
      end

      private

      def flush_pending_problems
        return '' if pending_problems.empty?

        pending_problems.shift(pending_problems.length).each_with_object(problems_header) do |(type, details), output|
          output << formatted_problem(type, details)
          printed_problems << [type, details]
        end
      end

      def problems_header
        printed_problems.empty? ? +"Problems:\n" : +''
      end

      def formatted_problem(type, details)
        number = printed_problems.length + 1
        "#{indent_numbered(format_problem(type, details), 2, number)}\n"
      end

      def make_summary_block
        report = summary_report.call(StringIO.new)
        suggestions = report.all_suggestions
        output = +"\n"
        output << report.stats
        output << "\n"
        output << report.snippets(suggestions) if suggestions.any?
        output
      end

      def make_progress_block(state)
        [
          '',
          make_bar(state.finished_scenarios, state.total_scenarios, 'scenarios'),
          make_bar(state.finished_steps, state.total_steps, 'steps'),
          make_status(state),
          ''
        ].join("\n")
      end

      def make_bar(finished, total, label)
        bar_width = [stream_columns - MIN_LEGEND_WIDTH, MAX_BAR_WIDTH].min
        ratio = total.positive? ? finished.to_f / total : 0
        filled_count = (ratio * bar_width).round
        empty_count = bar_width - filled_count
        "#{'█' * filled_count}#{'░' * empty_count} #{finished}/#{total} #{label}"
      end

      def stream_columns
        stream.respond_to?(:columns) && stream.columns ? stream.columns : 80
      end

      def make_status(state)
        case state.phase
        when :preparing then 'Getting ready...'
        when :running then "Running #{state.running_scenarios} scenarios..."
        when :done then 'Done'
        end
      end

      def render(content, initial)
        unless initial
          stream.move_cursor(0, -4) if stream.respond_to?(:move_cursor)
          stream.clear_screen_down if stream.respond_to?(:clear_screen_down)
        end
        stream.write(content)
      end

      def format_problem(type, details)
        Formatting.format_problem(type, details, options.fetch(:theme), stream)
      end

      def indent_numbered(text, by, number)
        baseline_indent = ' ' * by
        text.split("\n").each_with_index.map do |line, index|
          next '' if line.empty?

          index.zero? ? "#{baseline_indent}#{number}) #{line}" : "#{baseline_indent}   #{line}"
        end.join("\n")
      end
    end
  end
end
