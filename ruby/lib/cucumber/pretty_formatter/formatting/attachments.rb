# frozen_string_literal: true

module Cucumber
  module PrettyFormatter
    module Formatting
      module Attachments
        def format_attachment(attachment, theme, stream)
          content = if attachment.content_encoding == 'BASE64'
                      format_binary_attachment(attachment)
                    else
                      attachment.body
                    end
          TextBuilder.new(stream).append(content).build(theme[:attachment])
        end

        private

        def format_binary_attachment(attachment)
          bytes = (attachment.body.length / 4) * 3
          if attachment.file_name
            "Embedding #{attachment.file_name} [#{attachment.media_type} #{bytes} bytes]"
          else
            "Embedding [#{attachment.media_type} #{bytes} bytes]"
          end
        end
      end
    end
  end
end
