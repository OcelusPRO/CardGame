/** Builds the socket URL from the page origin, so it follows http and https alike. */
export function gameSocketUrl(code: string, origin: string = window.location.origin): string {
  const url = new URL(origin)
  url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:'
  url.pathname = `/ws/game/${code}`
  url.search = ''
  return url.toString()
}
