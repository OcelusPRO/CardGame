import { create } from 'zustand'
import type { ChatVotesView, GameView } from '../api/types'
import { GameSocket, type SocketStatus } from './GameSocket'
import type { ClientMessage } from './messages'
import { gameSocketUrl, spectatorSocketUrl } from './socketUrl'

/** Live chat tallies by answer id, refreshed on their own frame while the viewers vote. */
export type LiveChatVotes = Record<number, ChatVotesView>

interface GameStore {
  game: GameView | null
  chatVotes: LiveChatVotes
  status: SocketStatus
  lastError: string | null
  /** Why the server shut the door, when it did. Set with the `rejected` status. */
  rejection: string | null
  connect: (code: string) => void
  /**
   * Watches a table without taking a seat: the stream page. What comes back over this
   * link carries no hand and no seat, so the same store serves both screens.
   */
  spectate: (code: string) => void
  disconnect: () => void
  send: (message: ClientMessage) => void
  dismissError: () => void
}

/** Holds the current table and the only socket the app ever opens. */
export const useGameStore = create<GameStore>((set, get) => {
  let socket: GameSocket | null = null
  // Keyed by the address, not the code: the same table has two of them, and opening the
  // stream feed of a game this tab is already seated at must not be taken for a no-op.
  let connectedUrl: string | null = null

  return {
    game: null,
    chatVotes: {},
    status: 'closed',
    lastError: null,
    rejection: null,

    connect: (code) => open(gameSocketUrl(code)),

    spectate: (code) => open(spectatorSocketUrl(code)),

    disconnect: () => {
      socket?.close()
      socket = null
      connectedUrl = null
      set({ game: null, chatVotes: {}, status: 'closed', lastError: null, rejection: null })
    },

    send: (message) => socket?.send(message),

    dismissError: () => set({ lastError: null }),
  }

  function open(url: string) {
    if (connectedUrl === url && socket) return
    get().disconnect()
    connectedUrl = url
    socket = new GameSocket(url, {
      onStatus: (status, reason) =>
        set({ status, rejection: status === 'rejected' ? (reason ?? '') : null }),
      onMessage: (message) => {
        // A tally belongs to the round it was counted in. Any change of round — or of
        // step — makes it stale, and the snapshot carries the final numbers by then.
        if (message.type === 'state') {
          const previous = get().game
          const sameRound =
            previous?.round?.number === message.game.round?.number &&
            previous?.phase === message.game.phase
          set({ game: message.game, chatVotes: sameRound ? get().chatVotes : {} })
        }
        if (message.type === 'chat_votes') {
          set({
            chatVotes: Object.fromEntries(
              message.answers.map(({ id, count, voters }) => [id, { count, voters }]),
            ),
          })
        }
        if (message.type === 'error') set({ lastError: message.code })
      },
    })
    socket.open()
  }
})
