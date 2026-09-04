import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { SavedDeck } from './SavedDeck'
import { DeckBuilder } from './DeckBuilder'

function savedDeck(overrides: Partial<SavedDeck> = {}): SavedDeck {
  return {
    id: 'deck-1',
    name: 'Soirée entre amis',
    situations: ['Chez moi, on ne parle jamais de ____.'],
    punchlines: ['la honte de ma vie'],
    ...overrides,
  }
}

describe('DeckBuilder', () => {
  beforeEach(() => window.localStorage.clear())

  it('loads a saved deck into the editor for modification', async () => {
    window.localStorage.setItem('cardgame.decks', JSON.stringify([savedDeck()]))
    const user = userEvent.setup()

    render(<DeckBuilder packs={[]} disabled={false} onApply={vi.fn()} />)

    await user.selectOptions(screen.getByLabelText('Modifier un deck enregistré'), 'deck-1')

    expect(screen.getByPlaceholderText('Nom du deck')).toHaveValue('Soirée entre amis')
    expect(screen.getByText('Chez moi, on ne parle jamais de ____.')).toBeInTheDocument()
    expect(screen.getByText('la honte de ma vie')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Mettre à jour/ })).toBeInTheDocument()
  })

  it('overwrites the edited deck in place instead of creating a new one', async () => {
    window.localStorage.setItem('cardgame.decks', JSON.stringify([savedDeck()]))
    const user = userEvent.setup()

    render(<DeckBuilder packs={[]} disabled={false} onApply={vi.fn()} />)

    await user.selectOptions(screen.getByLabelText('Modifier un deck enregistré'), 'deck-1')
    const nameInput = screen.getByPlaceholderText('Nom du deck')
    await user.clear(nameInput)
    await user.type(nameInput, 'Soirée entre amis (v2)')
    await user.click(screen.getByRole('button', { name: /Mettre à jour/ }))

    const stored = JSON.parse(window.localStorage.getItem('cardgame.decks') ?? '[]') as SavedDeck[]
    expect(stored).toHaveLength(1)
    expect(stored[0]).toMatchObject({ id: 'deck-1', name: 'Soirée entre amis (v2)' })
  })

  it('cancels editing without touching the stored deck', async () => {
    window.localStorage.setItem('cardgame.decks', JSON.stringify([savedDeck()]))
    const user = userEvent.setup()

    render(<DeckBuilder packs={[]} disabled={false} onApply={vi.fn()} />)

    await user.selectOptions(screen.getByLabelText('Modifier un deck enregistré'), 'deck-1')
    await user.click(screen.getByRole('button', { name: /Annuler/ }))

    expect(screen.getByPlaceholderText('Nom du deck')).toHaveValue('')
    expect(screen.getByRole('button', { name: /Enregistrer/ })).toBeInTheDocument()
    const stored = JSON.parse(window.localStorage.getItem('cardgame.decks') ?? '[]') as SavedDeck[]
    expect(stored).toEqual([savedDeck()])
  })
})
