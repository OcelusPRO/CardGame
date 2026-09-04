const KEY = 'sansfiltres:auth-return'

/**
 * Signing in is a full page redirect, and it always lands back on the home page. Noting
 * where the player was first is what lets a guest connect their Twitch account from a
 * lobby without losing the table they were sitting at.
 */
export function rememberReturnPath(path = window.location.pathname + window.location.search) {
  try {
    sessionStorage.setItem(KEY, path)
  } catch {
    // A browser refusing storage simply loses the shortcut back; nothing else breaks.
  }
}

/** The remembered path, consumed once, and only ever a path of this very site. */
export function takeReturnPath(): string | null {
  try {
    const value = sessionStorage.getItem(KEY)
    sessionStorage.removeItem(KEY)
    return value && value.startsWith('/') && !value.startsWith('//') ? value : null
  } catch {
    return null
  }
}
