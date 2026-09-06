import type { GameView } from '../../api/types'
import type { LiveChatVotes } from '../../game/gameStore'
import type { ClientMessage } from '../../game/messages'
import { messages } from '../../game/messages'
import { DuelPanel } from './DuelPanel'
import { FinishedPanel } from './FinishedPanel'
import { LobbyPanel } from './LobbyPanel'
import { ResultPanel } from './ResultPanel'
import { SubmitPanel } from './SubmitPanel'
import { VotePanel } from './VotePanel'

interface Props {
  game: GameView
  send: (message: ClientMessage) => void
  /** The running Twitch tally, which arrives apart from the snapshot. */
  liveChatVotes?: LiveChatVotes
}

/** Picks the screen matching the current step. The server decides, the client obeys. */
export function GameBoard({ game, send, liveChatVotes }: Props) {
  switch (game.phase) {
    case 'LOBBY':
      return (
        <LobbyPanel
          game={game}
          onSettings={(patch) => send(messages.settings(patch))}
          onDeck={(deck) => send(messages.deck(deck))}
        />
      )
    case 'SUBMITTING':
      return (
        <SubmitPanel
          game={game}
          onPlayCards={(cardIds, fills) => send(messages.play(cardIds, fills))}
          onWriteAnswers={(texts) => send(messages.write(texts))}
        />
      )
    case 'SELECTING':
      // A ladder is judged two answers at a time, which is a different screen — whoever
      // is doing the judging, since that is a setting of its own.
      if (game.settings.selectionFormat === 'DUELS') {
        return (
          <DuelPanel
            game={game}
            onChoose={(answerId) => send(messages.choose(answerId))}
            liveChatVotes={liveChatVotes}
          />
        )
      }
      return (
        <VotePanel
          game={game}
          onChoose={(answerId) => send(messages.choose(answerId))}
          liveChatVotes={liveChatVotes}
        />
      )
    case 'ROUND_RESULT':
      return <ResultPanel game={game} onNext={() => send(messages.next())} />
    case 'FINISHED':
      return <FinishedPanel game={game} send={send} />
  }
}
