# frozen_string_literal: true

module Cucumber
  module PrettyFormatter
    module Styles
      CODES = {
        black_bright: [90, 39],
        blue: [34, 39],
        bold: [1, 22],
        bg_blue: [44, 49],
        cyan: [36, 39],
        dim: [2, 22],
        green: [32, 39],
        italic: [3, 23],
        magenta: [35, 39],
        red: [31, 39],
        yellow: [33, 39]
      }.freeze
    end

    CUCUMBER_STATUS_COLORS = {
      'AMBIGUOUS' => :magenta,
      'FAILED' => :red,
      'PASSED' => :green,
      'PENDING' => :cyan,
      'SKIPPED' => :yellow,
      'UNDEFINED' => :blue,
      'UNKNOWN' => :black_bright
    }.freeze

    CUCUMBER_STATUS_ICONS = {
      'AMBIGUOUS' => '✘',
      'FAILED' => '✘',
      'PASSED' => '✔',
      'PENDING' => '■',
      'SKIPPED' => '↷',
      'UNDEFINED' => '■',
      'UNKNOWN' => ' '
    }.freeze

    CUCUMBER_PROGRESS_ICONS = {
      'AMBIGUOUS' => 'A',
      'FAILED' => 'F',
      'PASSED' => '.',
      'PENDING' => 'P',
      'SKIPPED' => '-',
      'UNDEFINED' => 'U',
      'UNKNOWN' => '?'
    }.freeze

    CUCUMBER_THEME = {
      affix: :italic,
      attachment: :blue,
      feature: { keyword: :bold },
      location: :black_bright,
      status: {
        all: CUCUMBER_STATUS_COLORS,
        icon: CUCUMBER_STATUS_ICONS,
        progress: CUCUMBER_PROGRESS_ICONS
      },
      rule: { keyword: :bold },
      scenario: { keyword: :bold },
      step: { argument: :bold, keyword: :bold },
      symbol: { bullet: '•' }
    }.freeze

    DEMO_THEME = {
      affix: :italic,
      attachment: :blue,
      data_table: { all: :black_bright, border: :dim, content: :italic },
      doc_string: { all: :black_bright, content: :italic, delimiter: :dim, media_type: :bold },
      feature: { all: :bg_blue, keyword: :bold, name: :italic },
      location: :black_bright,
      status: { all: CUCUMBER_STATUS_COLORS },
      rule: { all: :bg_blue, keyword: :bold, name: :italic },
      scenario: { all: :bg_blue, keyword: :bold, name: :italic },
      step: { argument: :bold, keyword: :bold, text: :italic },
      tag: %i[yellow bold],
      symbol: { bullet: '•' }
    }.freeze

    NONE_THEME = {}.freeze

    PLAIN_THEME = {
      status: {
        icon: CUCUMBER_STATUS_ICONS,
        progress: CUCUMBER_PROGRESS_ICONS
      },
      symbol: { bullet: '-' }
    }.freeze
  end
end
