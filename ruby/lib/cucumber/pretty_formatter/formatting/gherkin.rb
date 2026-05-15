# frozen_string_literal: true

module Cucumber
  module PrettyFormatter
    module Formatting
      module Gherkin
        def format_feature_title(feature, theme, stream)
          TextBuilder.new(stream)
                     .append("#{feature.keyword}:", theme.dig(:feature, :keyword))
                     .append(' ')
                     .append(feature.name, theme.dig(:feature, :name))
                     .build(theme.dig(:feature, :all))
        end

        def format_rule_title(rule, theme, stream)
          TextBuilder.new(stream)
                     .append("#{rule.keyword}:", theme.dig(:rule, :keyword))
                     .append(' ')
                     .append(rule.name, theme.dig(:rule, :name))
                     .build(theme.dig(:rule, :all))
        end

        def format_pickle_title(pickle, scenario, theme, stream)
          TextBuilder.new(stream)
                     .append("#{scenario.keyword}:", theme.dig(:scenario, :keyword))
                     .append(' ')
                     .append(pickle.name || '', theme.dig(:scenario, :name))
                     .build(theme.dig(:scenario, :all))
        end

        def format_tags(tags, theme, stream)
          TextBuilder.new(stream).append(tags.map(&:name).join(' ')).build(theme[:tag])
        end

        def format_source_reference(source_reference, theme, stream)
          builder = TextBuilder.new(stream).append('# ')
          builder.append(source_reference.uri || '(unknown)')
          builder.append(":#{source_reference.location.line}") if source_reference.location
          builder.build(theme[:location])
        end

        def format_pickle_location(pickle, location, theme, stream)
          return nil unless location

          TextBuilder.new(stream).append("# #{pickle.uri}:#{location.line}").build(theme[:location])
        end
      end
    end
  end
end
