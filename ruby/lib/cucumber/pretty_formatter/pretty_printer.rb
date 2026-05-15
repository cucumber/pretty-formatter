# frozen_string_literal: true

module Cucumber
  module PrettyFormatter
    class PrettyPrinter < Printer
      DEFAULT_OPTIONS = {
        include_attachments: true,
        include_feature_line: true,
        include_rule_line: true,
        summarise: false,
        use_status_icon: true,
        theme: CUCUMBER_THEME,
        format_code: ->(snippet) { snippet }
      }.freeze

      def initialize(stream: $stdout, **options)
        super(stream: stream, **DEFAULT_OPTIONS.merge(options))
        @layout = PrettyPrinterLayout.new(query, self.options, stream)
        @renderer = PrettyPrinterRenderer.new(query, layout, stream, self.options)
      end

      def update(envelope)
        query.update(envelope)

        handle_test_case_started(envelope_value(envelope, :test_case_started))
        handle_attachment(envelope_value(envelope, :attachment))
        handle_test_step_finished(envelope_value(envelope, :test_step_finished))
        handle_test_run_finished(envelope_value(envelope, :test_run_finished))

        nil
      end

      private

      attr_reader :layout, :renderer

      def handle_test_case_started(test_case_started)
        return unless test_case_started

        layout.pre_calculate(test_case_started)
        renderer.handle_test_case_started(test_case_started)
      end

      def handle_attachment(attachment)
        renderer.handle_attachment(attachment) if attachment
      end

      def handle_test_step_finished(test_step_finished)
        renderer.handle_test_step_finished(test_step_finished) if test_step_finished
      end

      def handle_test_run_finished(test_run_finished)
        return unless test_run_finished

        renderer.handle_test_run_finished(test_run_finished)
        summarise if options.fetch(:summarise)
      end

      def summarise
        SummaryPrinter.summarise(query, stream: stream, **options)
      end

      def envelope_value(envelope, name)
        envelope.public_send(name) if envelope.respond_to?(name)
      end
    end
  end
end
