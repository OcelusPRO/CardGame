import type { PackAdminView } from '../../api/adminTypes'

/**
 * The plain-text shape of a deck: a short metadata header, then the situations and the
 * réponses each listed one per line under their own `## ` heading. It round-trips through
 * [formatDeckText] and [parseDeckText].
 */

const SITUATIONS = /^##\s*situations?\s*$/i
const ANSWERS = /^##\s*r[eé]ponses?\s*$/i
const META = /^#\s*(nom|description|modes)\s*:\s*(.*)$/i

export interface ParsedDeck {
  name?: string
  description?: string
  answerModeCards?: boolean
  answerModeFreeText?: boolean
  situations: string[]
  punchlines: string[]
}

type PackShape = Pick<
  PackAdminView,
  'name' | 'description' | 'answerModeCards' | 'answerModeFreeText'
>

export function formatDeckText(pack: PackShape, situations: string[], punchlines: string[]): string {
  const modes = [pack.answerModeCards && 'cartes', pack.answerModeFreeText && 'sans-limites']
    .filter(Boolean)
    .join(', ')
  return [
    `# Nom: ${pack.name}`,
    `# Description: ${pack.description}`,
    `# Modes: ${modes}`,
    '',
    '## Situations',
    ...situations,
    '',
    '## Réponses',
    ...punchlines,
    '',
  ].join('\n')
}

export function parseDeckText(raw: string): ParsedDeck {
  const parsed: ParsedDeck = { situations: [], punchlines: [] }
  let section: 'situations' | 'punchlines' | null = null

  for (const line of raw.split('\n')) {
    const text = line.trim()
    if (!text) continue

    if (SITUATIONS.test(text)) {
      section = 'situations'
      continue
    }
    if (ANSWERS.test(text)) {
      section = 'punchlines'
      continue
    }

    const meta = text.match(META)
    if (meta) {
      const value = meta[2].trim()
      if (meta[1].toLowerCase() === 'nom') parsed.name = value
      else if (meta[1].toLowerCase() === 'description') parsed.description = value
      else {
        parsed.answerModeCards = /cartes?/i.test(value)
        parsed.answerModeFreeText = /sans.?limites?|libre|free/i.test(value)
      }
      continue
    }

    if (text.startsWith('#')) continue // a stray comment line
    if (section === 'situations') parsed.situations.push(text)
    else if (section === 'punchlines') parsed.punchlines.push(text)
    // lines before the first heading are ignored on purpose
  }

  return parsed
}
