import { Attachment, AttachmentContentEncoding } from '@cucumber/messages'

import { TextBuilder } from '../TextBuilder'
import { Theme } from '../types'

export function formatAttachment(
  attachment: Attachment,
  theme: Theme,
  stream: NodeJS.WritableStream
): string {
  switch (attachment.contentEncoding) {
    case AttachmentContentEncoding.BASE64:
      return formatBase64Attachment(
        attachment.body,
        attachment.mediaType,
        attachment.fileName,
        theme,
        stream
      )
    case AttachmentContentEncoding.IDENTITY:
      return formatTextAttachment(attachment.body, theme, stream)
  }
}

function formatBase64Attachment(
  data: string,
  mediaType: string,
  fileName: string | undefined,
  theme: Theme,
  stream: NodeJS.WritableStream
): string {
  const builder = new TextBuilder(stream)
  const bytes = (data.length / 4) * 3
  if (fileName) {
    builder.append(`Embedding ${fileName} [${mediaType} ${bytes} bytes]`)
  } else {
    builder.append(`Embedding [${mediaType} ${bytes} bytes]`)
  }
  return builder.build(theme.attachment)
}

function formatTextAttachment(
  content: string,
  theme: Theme,
  stream: NodeJS.WritableStream
): string {
  return new TextBuilder(stream).append(content).build(theme.attachment)
}
