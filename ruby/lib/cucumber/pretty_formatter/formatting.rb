# frozen_string_literal: true

require_relative 'formatting/attachments'
require_relative 'formatting/counts_and_durations'
require_relative 'formatting/gherkin'
require_relative 'formatting/problems'
require_relative 'formatting/step_arguments'
require_relative 'formatting/step_titles'
require_relative 'formatting/text'

module Cucumber
  module PrettyFormatter
    module Formatting
      extend Attachments
      extend CountsAndDurations
      extend Gherkin
      extend Problems
      extend StepArguments
      extend StepTitles
      extend Text

      ORDERED_STATUSES = CountsAndDurations::ORDERED_STATUSES
    end
  end
end
