import type { ChatVotesView, GameView } from '../api/types'

/** One answer's live chat tally, as it arrives on its own frame. */
export interface ChatAnswerVotesView extends ChatVotesView {
  id: number
}

/** Everything the server may push on the game socket. */
export type ServerMessage =
  | { type: 'state'; game: GameView }
  | { type: 'chat_votes'; answers: ChatAnswerVotesView[] }
  | { type: 'error'; code: string }
  | { type: 'pong' }

/** Parses a frame, returning null rather than throwing on anything unexpected. */
export function parseServerMessage(raw: string): ServerMessage | null {
  try {
    const parsed = JSON.parse(raw) as ServerMessage
    return parsed && typeof parsed.type === 'string' ? parsed : null
  } catch {
    return null
  }
}
