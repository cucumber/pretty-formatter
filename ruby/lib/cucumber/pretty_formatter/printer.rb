# frozen_string_literal: true

module Cucumber
  module PrettyFormatter
    class Printer
      attr_reader :stream, :options, :query

      def initialize(stream: $stdout, **options)
        @stream = stream
        @options = options
        configure_stream_color
        @query = Cucumber::Query::Query.new
      end

      def update(envelope)
        query.update(envelope)
        nil
      end

      private

      def configure_stream_color
        return unless options.key?(:color)

        stream.instance_variable_set(TextBuilder::ANSI_COLOR_ENABLED_IVAR, options.fetch(:color))
      end
    end
  end
end
