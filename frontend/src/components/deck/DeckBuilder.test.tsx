import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { CardPackView } from '../../api/types'
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

function pack(overrides: Partial<CardPackView> = {}): CardPackView {
  return {
    id: 'classique',
    name: 'Soirée Classique',
    description: '',
    situationCount: 111,
    punchlineCount: 107,
    adultOnly: false,
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

  it('asks before deleting a deck, naming the one about to go', async () => {
    window.localStorage.setItem('cardgame.decks', JSON.stringify([savedDeck()]))
    const user = userEvent.setup()

    render(<DeckBuilder packs={[]} disabled={false} onApply={vi.fn()} />)

    await user.selectOptions(screen.getByLabelText('Supprimer un deck enregistré'), 'deck-1')

    expect(screen.getByRole('alert')).toHaveTextContent('Soirée entre amis')
    const stored = JSON.parse(window.localStorage.getItem('cardgame.decks') ?? '[]') as SavedDeck[]
    expect(stored).toEqual([savedDeck()])
  })

  it('deletes the deck once the host confirms, and plays on without it', async () => {
    window.localStorage.setItem('cardgame.decks', JSON.stringify([savedDeck()]))
    const user = userEvent.setup()
    const onApply = vi.fn()

    render(<DeckBuilder packs={[]} disabled={false} onApply={onApply} />)

    await user.selectOptions(screen.getByLabelText('Supprimer un deck enregistré'), 'deck-1')
    await user.click(screen.getByRole('button', { name: /Oui, supprimer/ }))

    expect(window.localStorage.getItem('cardgame.decks')).toBe('[]')
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    expect(onApply).toHaveBeenLastCalledWith({
      packIds: [],
      customSituations: [],
      customPunchlines: [],
    })
  })

  it('keeps the deck when the host backs out', async () => {
    window.localStorage.setItem('cardgame.decks', JSON.stringify([savedDeck()]))
    const user = userEvent.setup()

    render(<DeckBuilder packs={[]} disabled={false} onApply={vi.fn()} />)

    await user.selectOptions(screen.getByLabelText('Supprimer un deck enregistré'), 'deck-1')
    await user.click(screen.getByRole('button', { name: /Non, garder/ }))

    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    const stored = JSON.parse(window.localStorage.getItem('cardgame.decks') ?? '[]') as SavedDeck[]
    expect(stored).toEqual([savedDeck()])
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

  it('ticks nothing for a host on a brand new table', async () => {
    const onApply = vi.fn()

    render(<DeckBuilder packs={[pack()]} disabled={false} onApply={onApply} />)

    expect(screen.getByRole('button', { name: /Soirée Classique/ })).toHaveAttribute(
      'aria-pressed',
      'false',
    )
    expect(
      screen.getByText('Aucun deck sélectionné : la partie se jouera uniquement sur les cartes écrites ici.'),
    ).toBeInTheDocument()
    await waitFor(() =>
      expect(onApply).toHaveBeenCalledWith({ packIds: [], customSituations: [], customPunchlines: [] }),
    )
  })

  it('remembers the host pack choice for the next table', async () => {
    window.localStorage.setItem('cardgame.lastSelectedPackIds', JSON.stringify(['classique']))
    const onApply = vi.fn()

    render(<DeckBuilder packs={[pack()]} disabled={false} onApply={onApply} />)

    expect(screen.getByRole('button', { name: /Soirée Classique/ })).toHaveAttribute(
      'aria-pressed',
      'true',
    )
    await waitFor(() =>
      expect(onApply).toHaveBeenCalledWith({
        packIds: ['classique'],
        customSituations: [],
        customPunchlines: [],
      }),
    )
  })

  it('has a guest mirror the live pool instead of reading or writing its own memory', async () => {
    window.localStorage.setItem('cardgame.lastSelectedPackIds', JSON.stringify(['someone-elses-pick']))

    render(<DeckBuilder packs={[pack()]} disabled onApply={vi.fn()} />)

    // The pack is ticked because it is the one live pack on this table, not because of
    // what this browser's own storage says — a guest has nothing of their own to restore.
    expect(screen.getByRole('button', { name: /Soirée Classique/ })).toHaveAttribute(
      'aria-pressed',
      'true',
    )
    expect(window.localStorage.getItem('cardgame.lastSelectedPackIds')).toBe(
      JSON.stringify(['someone-elses-pick']),
    )
  })
})
