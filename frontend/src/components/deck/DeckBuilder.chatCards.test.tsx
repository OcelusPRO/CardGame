import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { ChatCardLogView, DeckInput } from '../../api/types'
import { DeckBuilder } from './DeckBuilder'
import type { SavedDeck } from './SavedDeck'

function aLog(overrides: Partial<ChatCardLogView> = {}): ChatCardLogView {
  return {
    situations: [{ id: 'chat-s-1', text: 'Le pire du stream, c\'est ____.' }],
    punchlines: [{ id: 'chat-p-1', text: 'un poulet rôti mal cuit' }],
    ...overrides,
  }
}

const SITUATIONS = 'Vos situations (une par ligne, utilisez ____ pour les trous)'
const PUNCHLINES = 'Vos réponses (une par ligne)'

/** What the host would have on screen: the boxes, as the deck they would be saved as. */
function boxes() {
  return {
    situations: (screen.getByLabelText(SITUATIONS) as HTMLTextAreaElement).value,
    punchlines: (screen.getByLabelText(PUNCHLINES) as HTMLTextAreaElement).value,
  }
}

describe('DeckBuilder, fed by a Twitch chat', () => {
  beforeEach(() => window.localStorage.clear())

  it('writes what the chat proposed into the host boxes, as ordinary lines', () => {
    render(
      <DeckBuilder packs={[]} disabled={false} gameCode="ABCDE" chatCards={aLog()} onApply={vi.fn()} />,
    )

    expect(boxes()).toEqual({
      situations: 'Le pire du stream, c\'est ____.',
      punchlines: 'un poulet rôti mal cuit',
    })
    expect(screen.getByText(/2 cartes proposées par le tchat/)).toBeInTheDocument()
  })

  it('does not write the same proposal twice when the table sends a new snapshot', () => {
    const { rerender } = render(
      <DeckBuilder packs={[]} disabled={false} gameCode="ABCDE" chatCards={aLog()} onApply={vi.fn()} />,
    )

    rerender(
      <DeckBuilder packs={[]} disabled={false} gameCode="ABCDE" chatCards={aLog()} onApply={vi.fn()} />,
    )

    expect(boxes().situations).toBe('Le pire du stream, c\'est ____.')
  })

  it('never brings back a line the host deleted, snapshot after snapshot', async () => {
    const user = userEvent.setup()
    const { rerender } = render(
      <DeckBuilder packs={[]} disabled={false} gameCode="ABCDE" chatCards={aLog()} onApply={vi.fn()} />,
    )

    await user.clear(screen.getByLabelText(SITUATIONS))
    rerender(
      <DeckBuilder packs={[]} disabled={false} gameCode="ABCDE" chatCards={aLog()} onApply={vi.fn()} />,
    )

    expect(boxes().situations).toBe('')
    expect(boxes().punchlines).toBe('un poulet rôti mal cuit')
  })

  it('keeps a refusal across a reload, since the log the server keeps only grows', () => {
    window.localStorage.setItem(
      'cardgame.chatCards.refused',
      JSON.stringify({ code: 'ABCDE', ids: ['chat-s-1'] }),
    )

    render(
      <DeckBuilder packs={[]} disabled={false} gameCode="ABCDE" chatCards={aLog()} onApply={vi.fn()} />,
    )

    expect(boxes()).toEqual({ situations: '', punchlines: 'un poulet rôti mal cuit' })
  })

  it('offers the proposals again on another table, whose refusals are its own', () => {
    window.localStorage.setItem(
      'cardgame.chatCards.refused',
      JSON.stringify({ code: 'ZZZZZ', ids: ['chat-s-1'] }),
    )

    render(
      <DeckBuilder packs={[]} disabled={false} gameCode="ABCDE" chatCards={aLog()} onApply={vi.fn()} />,
    )

    expect(boxes().situations).toBe('Le pire du stream, c\'est ____.')
  })

  it('saves what the chat wrote as a deck like any other, and plays it as one', async () => {
    const user = userEvent.setup()
    const onApply = vi.fn()
    render(
      <DeckBuilder packs={[]} disabled={false} gameCode="ABCDE" chatCards={aLog()} onApply={onApply} />,
    )

    await user.type(screen.getByPlaceholderText('Nom du deck'), 'Stream du 7')
    await user.click(screen.getByRole('button', { name: /Enregistrer/ }))

    const stored = JSON.parse(window.localStorage.getItem('cardgame.decks') ?? '[]') as SavedDeck[]
    expect(stored).toHaveLength(1)
    expect(stored[0]).toMatchObject({
      name: 'Stream du 7',
      situations: ['Le pire du stream, c\'est ____.'],
      punchlines: ['un poulet rôti mal cuit'],
    })
    // The lines moved into the deck, which is now ticked: the same paquet, dealt once.
    expect(boxes()).toEqual({ situations: '', punchlines: '' })
    expect(last(onApply)).toEqual({
      packIds: [],
      customSituations: ['Le pire du stream, c\'est ____.'],
      customPunchlines: ['un poulet rôti mal cuit'],
    })
  })

  it('grows the deck of the last stream with what this one wrote, without doubling it', async () => {
    window.localStorage.setItem(
      'cardgame.decks',
      JSON.stringify([
        {
          id: 'deck-1',
          name: 'Stream du 6',
          situations: ['Une vieille situation ____.'],
          punchlines: [],
        },
      ]),
    )
    const user = userEvent.setup()
    const onApply = vi.fn()
    render(
      <DeckBuilder packs={[]} disabled={false} gameCode="ABCDE" chatCards={aLog()} onApply={onApply} />,
    )

    await user.selectOptions(screen.getByLabelText('Modifier un deck enregistré'), 'deck-1')
    expect(boxes().situations).toBe('Le pire du stream, c\'est ____.\nUne vieille situation ____.')

    await user.click(screen.getByRole('button', { name: /Mettre à jour/ }))

    const stored = JSON.parse(window.localStorage.getItem('cardgame.decks') ?? '[]') as SavedDeck[]
    expect(stored).toHaveLength(1)
    expect(stored[0]).toMatchObject({
      id: 'deck-1',
      name: 'Stream du 6',
      situations: ['Le pire du stream, c\'est ____.', 'Une vieille situation ____.'],
      punchlines: ['un poulet rôti mal cuit'],
    })
    expect(last(onApply)?.customSituations).toEqual([
      'Le pire du stream, c\'est ____.',
      'Une vieille situation ____.',
    ])
  })
})

function last(onApply: ReturnType<typeof vi.fn>): DeckInput | undefined {
  return onApply.mock.calls.at(-1)?.[0] as DeckInput | undefined
}
