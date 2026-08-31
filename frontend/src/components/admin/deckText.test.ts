import { describe, expect, it } from 'vitest'
import { formatDeckText, parseDeckText } from './deckText'

const pack = {
  name: 'Base',
  description: 'Le pack de départ',
  answerModeCards: true,
  answerModeFreeText: false,
}

describe('deckText', () => {
  it('round-trips a pack through text', () => {
    const text = formatDeckText(pack, ['Le pire, c’est ____.'], ['un chat mouillé', 'la honte'])
    const parsed = parseDeckText(text)

    expect(parsed.name).toBe('Base')
    expect(parsed.description).toBe('Le pack de départ')
    expect(parsed.answerModeCards).toBe(true)
    expect(parsed.answerModeFreeText).toBe(false)
    expect(parsed.situations).toEqual(['Le pire, c’est ____.'])
    expect(parsed.punchlines).toEqual(['un chat mouillé', 'la honte'])
  })

  it('reads a minimal deck with just the two headings', () => {
    const parsed = parseDeckText('## Situations\nune situation\n\n## Reponses\nune réponse\n')

    expect(parsed.situations).toEqual(['une situation'])
    expect(parsed.punchlines).toEqual(['une réponse'])
  })

  it('ignores blank lines and lines before the first heading', () => {
    const parsed = parseDeckText('du bruit\n\n## Situations\n\nune seule\n')

    expect(parsed.situations).toEqual(['une seule'])
    expect(parsed.punchlines).toEqual([])
  })

  it('returns nothing usable when the headings are missing', () => {
    const parsed = parseDeckText('juste\ndes\nlignes')

    expect(parsed.situations).toEqual([])
    expect(parsed.punchlines).toEqual([])
  })
})
