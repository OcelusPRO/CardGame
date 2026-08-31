/** A situation text broken into what is printed and what has to be filled in. */
export interface SituationPart {
  kind: 'text' | 'blank'
  value: string
  blankIndex: number
}

const PLACEHOLDER = /_{2,}/g

/**
 * Splits the sentence around its holes so the card can render each hole as a real
 * blank, and drop the chosen answer inside it once the player picked one.
 */
export function splitSituation(text: string): SituationPart[] {
  const parts: SituationPart[] = []
  let cursor = 0
  let blankIndex = 0

  for (const match of text.matchAll(PLACEHOLDER)) {
    const start = match.index ?? 0
    if (start > cursor) parts.push({ kind: 'text', value: text.slice(cursor, start), blankIndex: -1 })
    parts.push({ kind: 'blank', value: '', blankIndex: blankIndex++ })
    cursor = start + match[0].length
  }

  if (cursor < text.length) parts.push({ kind: 'text', value: text.slice(cursor), blankIndex: -1 })
  if (blankIndex === 0) parts.push({ kind: 'blank', value: '', blankIndex: 0 })
  return parts
}

/**
 * Same split for a punchline card that carries its own holes. Unlike a situation, a card
 * with no hole is printed as it is — nothing is appended.
 */
export function splitAnswerBlanks(text: string): SituationPart[] {
  const parts: SituationPart[] = []
  let cursor = 0
  let blankIndex = 0

  for (const match of text.matchAll(PLACEHOLDER)) {
    const start = match.index ?? 0
    if (start > cursor) parts.push({ kind: 'text', value: text.slice(cursor, start), blankIndex: -1 })
    parts.push({ kind: 'blank', value: '', blankIndex: blankIndex++ })
    cursor = start + match[0].length
  }

  if (cursor < text.length) parts.push({ kind: 'text', value: text.slice(cursor), blankIndex: -1 })
  return parts
}

/** How many holes a punchline card carries. */
export function countBlanks(text: string): number {
  return (text.match(PLACEHOLDER) ?? []).length
}

/** The card text with the player's [fills] dropped into its holes, in order. */
export function fillAnswerBlanks(text: string, fills: string[]): string {
  let index = 0
  return text.replace(PLACEHOLDER, (hole) => {
    const fill = fills[index++]?.trim()
    return fill ? fill : hole
  })
}
