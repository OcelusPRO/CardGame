/** Builds the socket URL from the page origin, so it follows http and https alike. */
export function gameSocketUrl(code: string, origin: string = window.location.origin): string {
  return socketUrl(`/ws/game/${code}`, origin)
}

/**
 * The read-only feed of the same table, for the stream page. It is a different address
 * rather than a flag on the first one so the server can answer it without a session: what
 * comes back carries no hand, and no seat is held open by watching.
 */
export function spectatorSocketUrl(code: string, origin: string = window.location.origin): string {
  return socketUrl(`/ws/spectate/${code}`, origin)
}

function socketUrl(pathname: string, origin: string): string {
  const url = new URL(origin)
  url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:'
  url.pathname = pathname
  url.search = ''
  return url.toString()
}
