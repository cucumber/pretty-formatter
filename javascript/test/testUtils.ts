export function normalizeEol(value: string) {
  return value.replaceAll(/\r\n/g, '\n')
}
