# frozen_string_literal: true

module Cucumber
  module PrettyFormatter
    module Formatting
      module Problems
        PROBLEM_TYPE_LABELS = {
          'PARAMETER_TYPE' => 'Undefined parameter type',
          'GLOBAL_HOOK' => 'Global hook',
          'TEST_CASE' => 'Scenario',
          'TEST_RUN' => 'Test run'
        }.freeze

        HOOK_TYPE_LABELS = {
          'BEFORE_TEST_RUN' => 'BeforeAll',
          'AFTER_TEST_RUN' => 'AfterAll',
          'BEFORE_TEST_CASE' => 'Before',
          'AFTER_TEST_CASE' => 'After',
          'BEFORE_TEST_STEP' => 'BeforeStep',
          'AFTER_TEST_STEP' => 'AfterStep'
        }.freeze

        def format_error(message, status, theme, stream)
          TextBuilder.new(stream).append(message.strip).build(theme.dig(:status, :all, status), style_each_line: true)
        end

        def extract_reportable_message(test_step_result)
          status = test_step_result.status
          exception = test_step_result.exception
          message = test_step_result.message
          return exception&.stack_trace || exception&.message || message if status == 'FAILED'
          return exception&.message || message if %w[PENDING SKIPPED].include?(status)

          nil
        end

        def format_problem(type, details, theme, stream)
          TextBuilder.new(stream)
                     .append("#{PROBLEM_TYPE_LABELS.fetch(type)}:", theme[:affix])
                     .append(' ')
                     .append(details)
                     .build
        end

        def format_undefined_parameter_type(undefined_parameter_type)
          "'#{undefined_parameter_type.name}' in '#{undefined_parameter_type.expression}'"
        end

        def format_hook_title(hook, status, theme, stream)
          TextBuilder.new(stream)
                     .append(hook_label(hook), theme.dig(:step, :keyword))
                     .tap { |builder| builder.append(" (#{hook.name})", theme.dig(:step, :text)) if hook&.name }
                     .build(theme.dig(:status, :all, status))
        end

        def format_ambiguous_step(step_definitions, theme, stream, fallback_bullet: ' ')
          builder = TextBuilder.new(stream).append('Multiple matching step definitions found:')
          step_definitions.each do |step_definition|
            append_ambiguous_step_definition(builder, step_definition, theme, stream, fallback_bullet)
          end
          builder.build(nil, style_each_line: true)
        end

        private

        def hook_label(hook)
          hook&.type ? HOOK_TYPE_LABELS.fetch(hook.type) : 'Hook'
        end

        def append_ambiguous_step_definition(builder, step_definition, theme, stream, fallback_bullet)
          builder.append("\n  #{theme.dig(:symbol, :bullet) || fallback_bullet} ")
          builder.append(step_definition.pattern.source) if step_definition.pattern&.source
          location = format_source_reference(step_definition.source_reference, theme, stream)
          builder.append(' ').append(location) if location
        end
      end
    end
  end
end
