# frozen_string_literal: true

module Cucumber
  module PrettyFormatter
    class ProgressPrinter < Printer
      DEFAULT_OPTIONS = {
        include_attachments: true,
        summarise: false,
        theme: CUCUMBER_THEME
      }.freeze

      def initialize(stream: $stdout, **options)
        super(stream: stream, **DEFAULT_OPTIONS.merge(options))
      end

      def update(envelope)
        query.update(envelope)
        write_test_step_finished(envelope)
        write_test_run_hook_finished(envelope)
        stream.write("\n") if envelope_value(envelope, :test_run_finished)
        nil
      end

      private

      def write_test_step_finished(envelope)
        test_step_finished = envelope_value(envelope, :test_step_finished)
        write_status(test_step_finished.test_step_result.status) if test_step_finished
      end

      def write_test_run_hook_finished(envelope)
        test_run_hook_finished = envelope_value(envelope, :test_run_hook_finished)
        write_status(test_run_hook_finished.result.status) if test_run_hook_finished
      end

      def write_status(status)
        stream.write(Formatting.format_status_character(status, options.fetch(:theme), stream))
      end

      def envelope_value(envelope, name)
        envelope.public_send(name) if envelope.respond_to?(name)
      end
    end
  end
end
