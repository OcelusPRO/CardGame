/** A deck the player keeps in their own browser, never on the server. */
export interface SavedDeck {
  id: string
  name: string
  situations: string[]
  punchlines: string[]
}
