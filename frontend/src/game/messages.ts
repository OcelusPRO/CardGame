import type { DeckInput, GameSettingsInput } from '../api/types'

/** Everything the client may push on the game socket, mirroring the Kotlin sealed type. */
export type ClientMessage =
  | { type: 'play'; cardIds: string[]; fills: string[][] }
  | { type: 'write'; texts: string[] }
  | { type: 'choose'; answerId: number }
  | { type: 'settings'; settings: GameSettingsInput }
  | { type: 'deck'; deck: DeckInput }
  | { type: 'kick'; playerId: string }
  | { type: 'start' }
  | { type: 'next' }
  | { type: 'lobby' }
  | { type: 'leave' }
  | { type: 'ping' }

/** Small builders so screens never hand-write a message shape. */
export const messages = {
  play: (cardIds: string[], fills: string[][] = []): ClientMessage => ({ type: 'play', cardIds, fills }),
  write: (texts: string[]): ClientMessage => ({ type: 'write', texts }),
  choose: (answerId: number): ClientMessage => ({ type: 'choose', answerId }),
  settings: (settings: GameSettingsInput): ClientMessage => ({ type: 'settings', settings }),
  deck: (deck: DeckInput): ClientMessage => ({ type: 'deck', deck }),
  kick: (playerId: string): ClientMessage => ({ type: 'kick', playerId }),
  start: (): ClientMessage => ({ type: 'start' }),
  next: (): ClientMessage => ({ type: 'next' }),
  lobby: (): ClientMessage => ({ type: 'lobby' }),
  leave: (): ClientMessage => ({ type: 'leave' }),
  ping: (): ClientMessage => ({ type: 'ping' }),
}
