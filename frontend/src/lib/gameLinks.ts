/**
 * A game has one address and one only: `/game/CODE`. It is the page you play on, and the
 * page a newcomer lands on to take a seat, so the link in the address bar is the invitation.
 */
export function gamePath(code: string): string {
  return `/game/${code}`
}

export function gameUrl(code: string, origin: string = window.location.origin): string {
  return `${origin}${gamePath(code)}`
}
