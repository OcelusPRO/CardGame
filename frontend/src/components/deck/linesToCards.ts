/** One line, one card: the simplest editor that still feels deliberate. */
export function linesToCards(raw: string): string[] {
  return raw
    .split('\n')
    .map((line) => line.trim())
    .filter((line) => line.length > 0)
}

/**
 * [value] with [texts] appended as new lines, skipping the ones already written there.
 *
 * Both the chat and the host write into the same box, so the same idea can arrive twice:
 * a viewer proposing what the host already typed adds nothing, and a line the host is
 * still in the middle of editing is left exactly as it is.
 */
export function withLines(value: string, texts: string[]): string {
  const present = new Set(linesToCards(value))
  const added: string[] = []
  for (const text of texts) {
    const line = text.trim()
    if (line.length === 0 || present.has(line)) continue
    present.add(line)
    added.push(line)
  }
  if (added.length === 0) return value
  const head = value.replace(/\n+$/, '')
  return head.length === 0 ? added.join('\n') : `${head}\n${added.join('\n')}`
}

/** The lines an edit took out of the box — what a deletion amounts to, said as texts. */
export function removedLines(before: string, after: string): string[] {
  const left = new Set(linesToCards(after))
  return linesToCards(before).filter((line) => !left.has(line))
}

/** [value] without the lines saying any of [texts] — the exact undo of [withLines]. */
export function withoutLines(value: string, texts: string[]): string {
  const dropped = new Set(texts.map((text) => text.trim()))
  return value
    .split('\n')
    .filter((line) => !dropped.has(line.trim()))
    .join('\n')
    .trim()
}
