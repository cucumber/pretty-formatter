# frozen_string_literal: true

module Cucumber
  module PrettyFormatter
    # rubocop:disable Metrics/ClassLength
    class PrettyPrinterRenderer
      GHERKIN_INDENT_LENGTH = 2
      STEP_ARGUMENT_INDENT_LENGTH = 2
      ATTACHMENT_INDENT_LENGTH = 4
      ERROR_INDENT_LENGTH = 4

      def initialize(query, layout, stream, options)
        @query = query
        @layout = layout
        @stream = stream
        @options = options
        @encountered_features_and_rules = Set.new
      end

      def handle_test_case_started(test_case_started)
        resolved = resolve_scenario(test_case_started)
        scenario_indent = layout.scenario_indent_by(test_case_started)

        print_feature_header(resolved)
        print_tags(resolved.fetch(:pickle), scenario_indent)
        print_scenario(test_case_started, resolved, scenario_indent)
      end

      def handle_test_step_finished(test_step_finished)
        scenario_indent = layout.scenario_indent_by(test_step_finished)
        max_content_length = layout.max_content_length_by(test_step_finished)
        resolved = resolve_step(test_step_finished)
        if resolved
          print_step_line(test_step_finished, resolved, scenario_indent, max_content_length)
          print_step_argument(resolved.fetch(:pickle_step), scenario_indent)
          print_ambiguous_step(test_step_finished, resolved.fetch(:test_step), scenario_indent)
        end
        print_error(test_step_finished, scenario_indent)
      end

      def handle_attachment(attachment)
        return unless options.fetch(:include_attachments)

        scenario_indent = layout.scenario_indent_by(attachment)
        content = Formatting.format_attachment(attachment, options.fetch(:theme), stream)
        println Formatting.pad(
          Formatting.indent(content, scenario_indent + nested_attachment_indent)
        )
      end

      def handle_test_run_finished(test_run_finished)
        error = test_run_finished.exception&.stack_trace || test_run_finished.exception&.message
        println Formatting.format_error(error, 'FAILED', options.fetch(:theme), stream) if error
      end

      private

      attr_reader :query, :layout, :stream, :options, :encountered_features_and_rules

      def resolve_scenario(test_case_started)
        pickle = query.find_pickle_by(test_case_started)
        lineage = query.find_lineage_by(pickle)
        {
          pickle: pickle,
          location: query.find_location_of(pickle),
          scenario: lineage.fetch(:scenario),
          rule: lineage[:rule],
          feature: lineage.fetch(:feature)
        }
      end

      def resolve_step(test_step_finished)
        test_step = query.find_test_step_by(test_step_finished)
        pickle_step = query.find_pickle_step_by(test_step)
        return nil unless pickle_step

        {
          test_step: test_step,
          pickle_step: pickle_step,
          step: query.find_step_by(pickle_step),
          step_definition: query.find_unambiguous_step_definition_by(test_step)
        }
      end

      def print_feature_header(resolved)
        print_feature_line(resolved.fetch(:feature))
        print_rule_line(resolved.fetch(:rule))
        println
      end

      def print_feature_line(feature)
        if options.fetch(:include_feature_line) && !encountered_features_and_rules.include?(feature)
          println
          println Formatting.format_feature_title(feature, options.fetch(:theme), stream)
        end
        encountered_features_and_rules.add(feature)
      end

      def print_rule_line(rule)
        return unless rule

        if options.fetch(:include_rule_line) && !encountered_features_and_rules.include?(rule)
          println
          println Formatting.indent(
            Formatting.format_rule_title(rule, options.fetch(:theme), stream),
            GHERKIN_INDENT_LENGTH
          )
        end
        encountered_features_and_rules.add(rule)
      end

      def print_tags(pickle, scenario_indent)
        return unless pickle.tags.any?

        println Formatting.indent(Formatting.format_tags(pickle.tags, options.fetch(:theme), stream), scenario_indent)
      end

      def print_scenario(test_case_started, resolved, scenario_indent)
        print_scenario_line(
          resolved.fetch(:pickle),
          resolved.fetch(:scenario),
          resolved.fetch(:location),
          scenario_indent,
          layout.max_content_length_by(test_case_started)
        )
      end

      def print_scenario_line(pickle, scenario, location, scenario_indent, max_content_length)
        print_gherkin_line(
          Formatting.format_pickle_title(pickle, scenario, options.fetch(:theme), stream),
          Formatting.format_pickle_location(pickle, location, options.fetch(:theme), stream),
          scenario_indent,
          max_content_length
        )
      end

      def print_step_line(test_step_finished, resolved, scenario_indent, max_content_length)
        print_gherkin_line(
          formatted_step_title(test_step_finished, resolved),
          formatted_source_reference(resolved.fetch(:step_definition)&.source_reference),
          scenario_indent,
          max_content_length
        )
      end

      def formatted_step_title(test_step_finished, resolved)
        Formatting.indent(formatted_step_title_content(test_step_finished, resolved), GHERKIN_INDENT_LENGTH)
      end

      def formatted_step_title_content(test_step_finished, resolved)
        Formatting.format_step_title(
          resolved.fetch(:test_step),
          resolved.fetch(:pickle_step),
          resolved.fetch(:step),
          test_step_finished.test_step_result.status,
          use_status_icon?,
          options.fetch(:theme),
          stream
        )
      end

      def formatted_source_reference(source_reference)
        return nil unless source_reference

        Formatting.format_source_reference(source_reference, options.fetch(:theme), stream)
      end

      def print_step_argument(pickle_step, scenario_indent)
        return unless pickle_step.argument

        println Formatting.indent(
          Formatting.format_step_argument(pickle_step.argument, options.fetch(:theme), stream),
          scenario_indent + nested_step_argument_indent
        )
      end

      def print_gherkin_line(title, location, indent_by, max_content_length)
        output = title.dup
        if location
          padding = max_content_length - Formatting.unstyled(title).length
          output += Formatting.indent(location, padding + 1)
        end
        println Formatting.indent(output, indent_by)
      end

      def print_error(test_step_finished, scenario_indent)
        error = Formatting.extract_reportable_message(test_step_finished.test_step_result)
        return unless error

        println Formatting.indent(
          Formatting.format_error(error, test_step_finished.test_step_result.status, options.fetch(:theme), stream),
          scenario_indent + nested_error_indent
        )
      end

      def print_ambiguous_step(test_step_finished, test_step, scenario_indent)
        return unless test_step_finished.test_step_result.status == 'AMBIGUOUS'

        content = Formatting.format_ambiguous_step(
          query.find_step_definitions_by(test_step),
          options.fetch(:theme),
          stream
        )
        return if content.empty?

        println Formatting.indent(content, scenario_indent + nested_error_indent)
      end

      def nested_attachment_indent
        status_icon_indent + GHERKIN_INDENT_LENGTH + ATTACHMENT_INDENT_LENGTH
      end

      def nested_step_argument_indent
        status_icon_indent + GHERKIN_INDENT_LENGTH + STEP_ARGUMENT_INDENT_LENGTH
      end

      def nested_error_indent
        status_icon_indent + GHERKIN_INDENT_LENGTH + ERROR_INDENT_LENGTH
      end

      def status_icon_indent
        use_status_icon? ? GHERKIN_INDENT_LENGTH : 0
      end

      def use_status_icon?
        options.fetch(:use_status_icon) && options.fetch(:theme).dig(:status, :icon)
      end

      def println(text = '')
        stream.write("#{text}\n")
      end
    end
    # rubocop:enable Metrics/ClassLength
  end
end
