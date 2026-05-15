# frozen_string_literal: true

module Cucumber
  module PrettyFormatter
    class PrettyPrinterLayout
      GHERKIN_INDENT_LENGTH = 2

      def initialize(query, options, stream)
        @query = query
        @options = options
        @stream = stream
        @scenario_indent_by_test_case_started_id = {}
        @max_content_length_by_test_case_started_id = {}
      end

      def pre_calculate(test_case_started)
        pickle = query.find_pickle_by(test_case_started)
        lineage = query.find_lineage_by(pickle)
        test_case = query.find_test_case_by(test_case_started)

        max_content_length_by_test_case_started_id[test_case_started.id] = max_content_length(
          pickle,
          lineage.fetch(:scenario),
          test_case
        )
        scenario_indent_by_test_case_started_id[test_case_started.id] = scenario_indent(lineage)
      end

      def scenario_indent_by(element)
        if element.respond_to?(:test_case_started_id) && element.test_case_started_id
          scenario_indent_by_test_case_started_id[element.test_case_started_id] || 0
        elsif element.respond_to?(:test_case_id)
          scenario_indent_by_test_case_started_id[element.id] || 0
        else
          0
        end
      end

      def max_content_length_by(element)
        if element.respond_to?(:test_case_started_id)
          max_content_length_by_test_case_started_id[element.test_case_started_id] || 0
        else
          max_content_length_by_test_case_started_id[element.id] || 0
        end
      end

      private

      attr_reader :query, :options, :stream, :scenario_indent_by_test_case_started_id,
                  :max_content_length_by_test_case_started_id

      def max_content_length(pickle, scenario, test_case)
        [scenario_length(pickle, scenario), *step_lengths(test_case)].max
      end

      def scenario_length(pickle, scenario)
        Formatting.unstyled(
          Formatting.format_pickle_title(pickle, scenario, options.fetch(:theme), stream)
        ).length
      end

      def step_lengths(test_case)
        test_case.test_steps.filter_map do |test_step|
          next unless test_step.pickle_step_id

          step_length(test_step)
        end
      end

      def step_length(test_step)
        pickle_step = query.find_pickle_step_by(test_step)
        step = query.find_step_by(pickle_step)

        Formatting.indent(
          Formatting.unstyled(formatted_unknown_step_title(test_step, pickle_step, step)),
          GHERKIN_INDENT_LENGTH
        ).length
      end

      def formatted_unknown_step_title(test_step, pickle_step, step)
        Formatting.format_step_title(
          test_step,
          pickle_step,
          step,
          'UNKNOWN',
          use_status_icon?,
          options.fetch(:theme),
          stream
        )
      end

      def scenario_indent(lineage)
        return 0 unless options.fetch(:include_feature_line)

        GHERKIN_INDENT_LENGTH + rule_indent(lineage)
      end

      def rule_indent(lineage)
        options.fetch(:include_rule_line) && lineage[:rule] ? GHERKIN_INDENT_LENGTH : 0
      end

      def use_status_icon?
        options.fetch(:use_status_icon) && options.fetch(:theme).dig(:status, :icon)
      end
    end
  end
end
