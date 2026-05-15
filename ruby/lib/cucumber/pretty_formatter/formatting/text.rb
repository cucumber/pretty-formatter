# frozen_string_literal: true

module Cucumber
  module PrettyFormatter
    module Formatting
      module Text
        def format_status_character(status, theme, stream)
          character = theme.dig(:status, :progress, status) || ' '
          TextBuilder.new(stream).append(character).build(theme.dig(:status, :all, status))
        end

        def indent(text, by)
          text.split("\n", -1).map { |line| (' ' * by) + line }.join("\n")
        end

        def pad(text)
          "\n#{text}\n"
        end

        def unstyled(text)
          text.gsub(/\e\[[0-9;]*m/, '')
        end
      end
    end
  end
end
