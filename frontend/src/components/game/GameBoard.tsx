import type { GameView } from '../../api/types'
import type { ClientMessage } from '../../game/messages'
import { messages } from '../../game/messages'
import { FinishedPanel } from './FinishedPanel'
import { LobbyPanel } from './LobbyPanel'
import { ResultPanel } from './ResultPanel'
import { SubmitPanel } from './SubmitPanel'
import { VotePanel } from './VotePanel'

interface Props {
  game: GameView
  send: (message: ClientMessage) => void
}

/** Picks the screen matching the current step. The server decides, the client obeys. */
export function GameBoard({ game, send }: Props) {
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
      return <VotePanel game={game} onChoose={(answerId) => send(messages.choose(answerId))} />
    case 'ROUND_RESULT':
      return <ResultPanel game={game} onNext={() => send(messages.next())} />
    case 'FINISHED':
      return <FinishedPanel game={game} send={send} />
  }
}
