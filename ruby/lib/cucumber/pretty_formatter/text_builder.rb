# frozen_string_literal: true

module Cucumber
  module PrettyFormatter
    class TextBuilder
      ANSI_COLOR_ENABLED_IVAR = :@cucumber_pretty_formatter_ansi_color_enabled

      def initialize(stream = nil, color: nil)
        @stream = stream
        @color = color
        @buffer = +''
      end

      def append(text, style = nil)
        @buffer << apply_style(text.to_s, style)
        self
      end

      def build(style = nil, style_each_line: false)
        return apply_style(@buffer, style) unless style_each_line

        @buffer.split("\n", -1).map { |line| apply_style(line, style) }.join("\n")
      end

      def to_s
        build
      end

      private

      def apply_style(text, style)
        styles = Array(style).compact
        return text if styles.empty? || !color_enabled?

        codes = styles.map { |single_style| Styles::CODES.fetch(single_style) }
        open_codes = codes.map { |open_code, _close_code| "\e[#{open_code}m" }.join
        close_codes = codes.reverse.map { |_open_code, close_code| "\e[#{close_code}m" }.join
        "#{open_codes}#{text}#{close_codes}"
      end

      def color_enabled?
        return @color unless @color.nil?

        configured_color = stream_configured_color
        return configured_color unless configured_color.nil?
        return false if ENV.key?('NO_COLOR')

        @stream.respond_to?(:tty?) && @stream.tty?
      end

      def stream_configured_color
        return nil unless @stream&.instance_variable_defined?(ANSI_COLOR_ENABLED_IVAR)

        @stream.instance_variable_get(ANSI_COLOR_ENABLED_IVAR)
      end
    end
  end
end
