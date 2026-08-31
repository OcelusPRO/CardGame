import { parseServerMessage, type ServerMessage } from './serverMessages'
import type { ClientMessage } from './messages'
import { gameSocketUrl } from './socketUrl'

export type SocketStatus = 'connecting' | 'open' | 'closed'

interface Handlers {
  onMessage: (message: ServerMessage) => void
  onStatus: (status: SocketStatus) => void
}

const RECONNECT_STEPS_MS = [500, 1000, 2000, 4000, 8000]
const PING_INTERVAL_MS = 20000

/**
 * The link to a game. It reconnects on its own with a growing delay, and pings so a
 * phone waking up from sleep notices a dead link instead of showing a frozen table.
 */
export class GameSocket {
  private socket: WebSocket | null = null
  private attempt = 0
  private pingTimer: ReturnType<typeof setInterval> | null = null
  private retryTimer: ReturnType<typeof setTimeout> | null = null
  private closedByUs = false

  constructor(
    private readonly code: string,
    private readonly handlers: Handlers,
  ) {}

  open(): void {
    this.closedByUs = false
    this.handlers.onStatus('connecting')
    const socket = new WebSocket(gameSocketUrl(this.code))
    this.socket = socket
    socket.onopen = () => this.onOpen()
    socket.onmessage = (event) => this.onMessage(event)
    socket.onclose = () => this.onClose()
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

  private onClose(): void {
    this.stopTimers()
    this.handlers.onStatus('closed')
    if (this.closedByUs) return
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
