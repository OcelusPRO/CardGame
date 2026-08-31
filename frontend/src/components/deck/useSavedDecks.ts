import { useCallback } from 'react'
import { useLocalStorage } from '../../hooks/useLocalStorage'
import type { SavedDeck } from './SavedDeck'

const STORAGE_KEY = 'cardgame.decks'

/**
 * Custom decks live in the browser only, which is exactly what was asked: the server
 * stores the official cards, the player keeps their own.
 */
export function useSavedDecks() {
  const [decks, setDecks] = useLocalStorage<SavedDeck[]>(STORAGE_KEY, [])

  const save = useCallback(
    (name: string, situations: string[], punchlines: string[]) => {
      const deck: SavedDeck = { id: crypto.randomUUID(), name, situations, punchlines }
      setDecks([...decks.filter((existing) => existing.name !== name), deck])
      return deck
    },
    [decks, setDecks],
  )

  const remove = useCallback(
    (id: string) => setDecks(decks.filter((deck) => deck.id !== id)),
    [decks, setDecks],
  )

  return { decks, save, remove }
}
