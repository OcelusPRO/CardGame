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

/**
 * The other address of the same table: `/spec/CODE`, which shows the game without ever
 * showing a hand. It is what a streamer puts on screen — as a window, or as a browser
 * source in OBS — so their chat follows the round without reading the cards they hold.
 *
 * It takes no seat and asks for no session: a capture source has no cookie, and a viewer
 * who is only watching should not have to sign in to do it.
 *
 * `overlay` adds the marker the page reads to draw itself for a video layer. The lobby
 * offers the plain link only — the overlay is for the few hosts who build a scene in OBS,
 * and they can add `?overlay=1` themselves rather than every other host paying for a
 * second button they will never press.
 */
export function spectatePath(code: string, overlay = false): string {
  return overlay ? `/spec/${code}?overlay=1` : `/spec/${code}`
}

export function spectateUrl(
  code: string,
  overlay = false,
  origin: string = window.location.origin,
): string {
  return `${origin}${spectatePath(code, overlay)}`
}

/**
 * Whether the stream page should draw itself for a video layer rather than for a browser:
 * no page furniture, no ground of its own, just the table.
 */
export function isOverlay(search: string): boolean {
  return new URLSearchParams(search).get('overlay') === '1'
}
