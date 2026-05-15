# frozen_string_literal: true

module Cucumber
  module PrettyFormatter
    module Formatting
      module StepTitles
        StepTitle = Data.define(:test_step, :pickle_step, :step, :status, :use_status_icon, :theme, :stream)

        def format_step_title(*)
          build_step_title(StepTitle.new(*))
        end

        private

        def build_step_title(context)
          builder = TextBuilder.new(context.stream)
          append_status_icon(builder, context) if context.use_status_icon
          builder.append(step_title_content(context)).build
        end

        def step_title_content(context)
          TextBuilder.new(context.stream)
                     .append(context.step.keyword, context.theme.dig(:step, :keyword))
                     .append(format_step_text(context.test_step, context.pickle_step, context.theme, context.stream))
                     .build(context.theme.dig(:status, :all, context.status))
        end

        def append_status_icon(builder, context)
          builder.append(
            context.theme.dig(:status, :icon, context.status) || ' ',
            context.theme.dig(:status, :all, context.status)
          ).append(' ')
        end

        def format_step_text(test_step, pickle_step, theme, stream)
          builder = TextBuilder.new(stream)
          lists = test_step.step_match_arguments_lists
          if lists && lists.length == 1
            append_highlighted_step_text(builder, lists.first.step_match_arguments, pickle_step, theme)
          else
            builder.append(pickle_step.text, theme.dig(:step, :text))
          end
          builder.build
        end

        def append_highlighted_step_text(builder, arguments, pickle_step, theme)
          current_index = 0
          arguments.each do |argument|
            next current_index unless append_step_match(builder, argument.group, pickle_step, current_index, theme)

            current_index = argument.group.start + argument.group.value.length
          end
          append_remaining_step_text(builder, pickle_step, current_index, theme)
        end

        def append_step_match(builder, group, pickle_step, current_index, theme)
          return false if group.value.nil? || group.start.nil?

          builder.append(pickle_step.text[current_index...group.start], theme.dig(:step, :text))
                 .append(group.value, theme.dig(:step, :argument))
        end

        def append_remaining_step_text(builder, pickle_step, current_index, theme)
          return if current_index == pickle_step.text.length

          builder.append(pickle_step.text[current_index..], theme.dig(:step, :text))
        end
      end
    end
  end
end
