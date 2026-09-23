import type { AvatarInput, DeckInput, GameSettingsInput } from '../api/types'

/** Everything the client may push on the game socket, mirroring the Kotlin sealed type. */
export type ClientMessage =
  | { type: 'play'; cardIds: string[]; fills: string[][] }
  | { type: 'write'; texts: string[] }
  | { type: 'choose'; answerId: number }
  | { type: 'settings'; settings: GameSettingsInput }
  | { type: 'deck'; deck: DeckInput }
  | { type: 'kick'; playerId: string }
  | { type: 'step_aside'; heir?: string }
  | { type: 'add_seat'; nickname: string; avatar: AvatarInput }
  | { type: 'seat'; playerId: string | null }
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
  /** Trade the seat for the stream page; a host names who takes the crown. */
  stepAside: (heir?: string): ClientMessage => ({ type: 'step_aside', heir }),
  /** One more player around a shared device. */
  addSeat: (nickname: string, avatar: AvatarInput): ClientMessage => ({ type: 'add_seat', nickname, avatar }),
  /** Hand a shared device to a player, or turn it back to the table with `null`. */
  seat: (playerId: string | null): ClientMessage => ({ type: 'seat', playerId }),
  start: (): ClientMessage => ({ type: 'start' }),
  next: (): ClientMessage => ({ type: 'next' }),
  lobby: (): ClientMessage => ({ type: 'lobby' }),
  leave: (): ClientMessage => ({ type: 'leave' }),
  ping: (): ClientMessage => ({ type: 'ping' }),
}
