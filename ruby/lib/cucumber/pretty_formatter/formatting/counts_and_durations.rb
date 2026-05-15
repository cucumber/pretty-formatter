# frozen_string_literal: true

module Cucumber
  module PrettyFormatter
    module Formatting
      module CountsAndDurations
        ORDERED_STATUSES = %w[UNKNOWN PASSED SKIPPED PENDING UNDEFINED AMBIGUOUS FAILED].freeze

        def format_counts(noun, counts, theme, stream)
          total = counts.values.sum
          builder = TextBuilder.new(stream)
          builder.append("#{total} #{total == 1 ? noun.fetch(0) : noun.fetch(1)}")

          append_status_counts(builder, counts, theme) if total.positive?

          builder.build
        end

        def format_durations(test_run_duration, execution_durations)
          test_run_milliseconds = duration_to_milliseconds(test_run_duration)
          execution_milliseconds = execution_durations.sum { |duration| duration_to_milliseconds(duration) }

          "#{format_duration_milliseconds(test_run_milliseconds)} " \
            "(#{format_duration_milliseconds(execution_milliseconds)} executing your code)"
        end

        private

        def append_status_counts(builder, counts, theme)
          builder.append(' (')
          first = true
          ORDERED_STATUSES.each do |status|
            count = counts[status]
            next unless count&.positive?

            builder.append(', ') unless first
            builder.append("#{count} #{status.downcase}", theme.dig(:status, :all, status))
            first = false
          end
          builder.append(')')
        end

        def duration_to_milliseconds(duration)
          return 0 unless duration

          (duration.seconds * 1_000) + (duration.nanos / 1_000_000.0)
        end

        def format_duration_milliseconds(milliseconds)
          minutes = (milliseconds / 60_000).floor
          remaining_seconds = (milliseconds - (minutes * 60_000)) / 1_000.0
          seconds = remaining_seconds.floor
          milliseconds = (milliseconds - (minutes * 60_000) - (seconds * 1_000)).round
          fractional = milliseconds.to_s
          formatted_seconds = fractional.empty? ? "#{seconds}.0" : "#{seconds}.#{fractional}"
          "#{minutes}m #{formatted_seconds}s"
        end
      end
    end
  end
end
