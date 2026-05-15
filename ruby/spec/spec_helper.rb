# frozen_string_literal: true

require 'stringio'
require 'cucumber/messages'

$LOAD_PATH.unshift File.expand_path('../lib', __dir__)
require 'cucumber/pretty_formatter'

def with_environment(updates)
  previous = updates.to_h { |key, _value| [key, ENV.fetch(key, nil)] }
  updates.each { |key, value| ENV[key] = value }
  yield
ensure
  previous.each do |key, value|
    value.nil? ? ENV.delete(key) : ENV[key] = value
  end
end

RSpec.configure do |config|
  config.disable_monkey_patching!
  config.example_status_persistence_file_path = '.rspec_status'
  config.expect_with :rspec do |expectations|
    expectations.syntax = :expect
  end
end
