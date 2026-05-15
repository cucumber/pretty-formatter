# frozen_string_literal: true

version = File.read(File.expand_path('VERSION', __dir__)).strip

Gem::Specification.new do |s|
  s.name        = 'cucumber-pretty-formatter'
  s.version     = version
  s.authors     = ['Matt Wynne']
  s.description = 'Rich formatting of Cucumber progress and results for the terminal'
  s.summary     = "cucumber-pretty-formatter-#{s.version}"
  s.email       = 'hello@cucumber.io'
  s.homepage    = 'https://github.com/cucumber/pretty-formatter#readme'
  s.platform    = Gem::Platform::RUBY
  s.license     = 'MIT'
  s.required_ruby_version = '>= 3.2'
  s.required_rubygems_version = '>= 3.2.8'

  s.metadata = {
    'bug_tracker_uri' => 'https://github.com/cucumber/pretty-formatter/issues',
    'changelog_uri' => 'https://github.com/cucumber/pretty-formatter/blob/main/CHANGELOG.md',
    'documentation_uri' => 'https://github.com/cucumber/pretty-formatter/tree/main/ruby',
    'rubygems_mfa_required' => 'true',
    'source_code_uri' => 'https://github.com/cucumber/pretty-formatter'
  }

  s.add_dependency 'cucumber-messages', '>= 32.0.0', '< 33.0.0'
  s.add_dependency 'cucumber-query', '>= 15.0.0', '< 16.0.0'

  s.add_development_dependency 'rake', '~> 13.1'
  s.add_development_dependency 'rspec', '~> 3.13'
  s.add_development_dependency 'rubocop', '~> 1.80'
  s.add_development_dependency 'rubocop-performance', '~> 1.24'
  s.add_development_dependency 'rubocop-rake', '~> 0.6'
  s.add_development_dependency 'rubocop-rspec', '~> 3.7'

  s.files = Dir['README.md', 'VERSION', 'lib/**/*']
  s.rdoc_options = ['--charset=UTF-8']
  s.require_path = 'lib'
end
