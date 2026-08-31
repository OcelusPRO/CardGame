/** One line, one card: the simplest editor that still feels deliberate. */
export function linesToCards(raw: string): string[] {
  return raw
    .split('\n')
    .map((line) => line.trim())
    .filter((line) => line.length > 0)
}
