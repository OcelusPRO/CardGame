/**
 * The invitation address of a game: `/game/CODE`. It is where a newcomer lands to take a
 * seat, so the link is the whole invitation. Once seated, the player's address bar drops
 * to the bare `/game` (see `activeGame`) — the code is an invite, not a badge to wear.
 */
export function gamePath(code: string): string {
  return `/game/${code}`
}

export function gameUrl(code: string, origin: string = window.location.origin): string {
  return `${origin}${gamePath(code)}`
}
