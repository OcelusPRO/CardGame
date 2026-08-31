import { create } from 'zustand'
import type { GameView } from '../api/types'
import { GameSocket, type SocketStatus } from './GameSocket'
import type { ClientMessage } from './messages'

interface GameStore {
  game: GameView | null
  status: SocketStatus
  lastError: string | null
  connect: (code: string) => void
  disconnect: () => void
  send: (message: ClientMessage) => void
  dismissError: () => void
}

/** Holds the current table and the only socket the app ever opens. */
export const useGameStore = create<GameStore>((set, get) => {
  let socket: GameSocket | null = null
  let connectedCode: string | null = null

  return {
    game: null,
    status: 'closed',
    lastError: null,

    connect: (code) => {
      if (connectedCode === code && socket) return
      get().disconnect()
      connectedCode = code
      socket = new GameSocket(code, {
        onStatus: (status) => set({ status }),
        onMessage: (message) => {
          if (message.type === 'state') set({ game: message.game })
          if (message.type === 'error') set({ lastError: message.code })
        },
      })
      socket.open()
    },

    disconnect: () => {
      socket?.close()
      socket = null
      connectedCode = null
      set({ game: null, status: 'closed', lastError: null })
    },

    send: (message) => socket?.send(message),

    dismissError: () => set({ lastError: null }),
  }
})
