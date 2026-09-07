import { parseServerMessage, type ServerMessage } from './serverMessages'
import type { ClientMessage } from './messages'

export type SocketStatus = 'connecting' | 'open' | 'closed' | 'rejected'

interface Handlers {
  onMessage: (message: ServerMessage) => void
  onStatus: (status: SocketStatus, reason?: string) => void
}

const RECONNECT_STEPS_MS = [500, 1000, 2000, 4000, 8000]
const PING_INTERVAL_MS = 20000

/**
 * `1008 Policy Violation` is what the server answers when the seat itself is the problem:
 * no session, unknown code, or a table this browser is not sitting at. Retrying cannot
 * fix any of those, so the link is dropped for good and the screen says why.
 */
const POLICY_VIOLATION = 1008

/**
 * The link to a game. It reconnects on its own with a growing delay, and pings so a
 * phone waking up from sleep notices a dead link instead of showing a frozen table.
 *
 * It is given the address rather than the code, because there are two of them: the seat
 * socket a player holds, and the read-only feed the stream page watches. Everything below
 * — the retries, the ping, the final refusal — is the same for both.
 */
export class GameSocket {
  private socket: WebSocket | null = null
  private attempt = 0
  private pingTimer: ReturnType<typeof setInterval> | null = null
  private retryTimer: ReturnType<typeof setTimeout> | null = null
  private closedByUs = false

  constructor(
    private readonly url: string,
    private readonly handlers: Handlers,
  ) {}

  open(): void {
    this.closedByUs = false
    this.handlers.onStatus('connecting')
    const socket = new WebSocket(this.url)
    this.socket = socket
    socket.onopen = () => this.onOpen()
    socket.onmessage = (event) => this.onMessage(event)
    socket.onclose = (event) => this.onClose(event)
    socket.onerror = () => socket.close()
  }

  send(message: ClientMessage): void {
    if (this.socket?.readyState === WebSocket.OPEN) this.socket.send(JSON.stringify(message))
  }

  close(): void {
    this.closedByUs = true
    this.stopTimers()
    this.socket?.close()
    this.socket = null
  }

  private onOpen(): void {
    this.attempt = 0
    this.handlers.onStatus('open')
    this.pingTimer = setInterval(() => this.send({ type: 'ping' }), PING_INTERVAL_MS)
  }

  private onMessage(event: MessageEvent): void {
    const message = parseServerMessage(String(event.data))
    if (message) this.handlers.onMessage(message)
  }

  private onClose(event: CloseEvent): void {
    this.stopTimers()
    if (this.closedByUs) return this.handlers.onStatus('closed')
    // A refusal is final: reconnecting would only get refused again, once every eight
    // seconds, on a table that will never open.
    if (event.code === POLICY_VIOLATION) {
      this.closedByUs = true
      this.socket = null
      return this.handlers.onStatus('rejected', event.reason || undefined)
    }
    this.handlers.onStatus('closed')
    const delay = RECONNECT_STEPS_MS[Math.min(this.attempt, RECONNECT_STEPS_MS.length - 1)]
    this.attempt += 1
    this.retryTimer = setTimeout(() => this.open(), delay)
  }

  private stopTimers(): void {
    if (this.pingTimer) clearInterval(this.pingTimer)
    if (this.retryTimer) clearTimeout(this.retryTimer)
    this.pingTimer = null
    this.retryTimer = null
  }
}
