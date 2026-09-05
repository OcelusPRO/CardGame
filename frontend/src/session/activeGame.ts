const KEY = 'sansfiltres:active-game'

/**
 * The table a player is currently sitting at. Once a seat is taken the code leaves the
 * address bar — a lobby is often on a screen other people can see — but a reload still
 * has to land back at the same table, so the code is kept here for the life of the tab.
 */
export function rememberActiveGame(code: string) {
  try {
    sessionStorage.setItem(KEY, code)
  } catch {
    // A browser refusing storage just means a reload drops back to the home page.
  }
}

/** The remembered code, or an empty string when there is nothing to go back to. */
export function readActiveGame(): string {
  try {
    return sessionStorage.getItem(KEY) ?? ''
  } catch {
    return ''
  }
}

export function forgetActiveGame() {
  try {
    sessionStorage.removeItem(KEY)
  } catch {
    // Nothing stored, nothing to clear.
  }
}
