# frozen_string_literal: true

module Cucumber
  module PrettyFormatter
    module Formatting
      module StepArguments
        def format_step_argument(argument, theme, stream)
          return format_doc_string(argument.doc_string, theme, stream) if argument.doc_string
          return format_data_table(argument.data_table, theme, stream) if argument.data_table

          raise 'PickleStepArgument must have one of dataTable or docString'
        end

        def format_doc_string(doc_string, theme, stream)
          builder = TextBuilder.new(stream).append('"""', theme.dig(:doc_string, :delimiter))
          append_doc_string_media_type(builder, doc_string, theme)
          append_doc_string_content(builder, doc_string, theme)
          builder.append('"""', theme.dig(:doc_string, :delimiter))
          builder.build(theme.dig(:doc_string, :all), style_each_line: true)
        end

        def format_data_table(data_table, theme, stream)
          widths = data_table_column_widths(data_table)
          rows = data_table.rows.map do |row|
            format_data_table_row(row, widths, theme, stream)
          end
          TextBuilder.new(stream).append(rows.join("\n")).build(theme.dig(:data_table, :all), style_each_line: true)
        end

        private

        def append_doc_string_media_type(builder, doc_string, theme)
          builder.append(doc_string.media_type, theme.dig(:doc_string, :media_type)) if doc_string.media_type
          builder.append("\n")
        end

        def append_doc_string_content(builder, doc_string, theme)
          doc_string.content.split("\n", -1).each do |line|
            builder.append(line, theme.dig(:doc_string, :content)).append("\n")
          end
        end

        def data_table_column_widths(data_table)
          widths = []
          data_table.rows.each do |row|
            row.cells.each_with_index { |cell, index| widths[index] = [widths[index] || 0, cell.value.length].max }
          end
          widths
        end

        def format_data_table_row(row, widths, theme, stream)
          builder = TextBuilder.new(stream).append('|', theme.dig(:data_table, :border))
          row.cells.each_with_index do |cell, index|
            builder.append(" #{cell.value.ljust(widths[index])} ", theme.dig(:data_table, :content))
                   .append('|', theme.dig(:data_table, :border))
          end
          builder.build
        end
      end
    end
  end
end
