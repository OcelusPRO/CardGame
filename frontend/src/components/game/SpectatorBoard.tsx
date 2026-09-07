import type { GameView } from '../../api/types'
import type { LiveChatVotes } from '../../game/gameStore'
import { Avatar } from '../avatar/Avatar'
import { SituationCard } from '../cards/SituationCard'
import { Panel } from '../ui/Panel'
import { DuelPanel } from './DuelPanel'
import { FinishedPanel } from './FinishedPanel'
import { ResultPanel } from './ResultPanel'
import { RoundStage } from './RoundStage'
import { VotePanel } from './VotePanel'

interface Props {
  game: GameView
  /** The running Twitch tally, which arrives apart from the snapshot. */
  liveChatVotes?: LiveChatVotes
}

/** Nothing on this screen acts, so every handler the panels ask for goes nowhere. */
const inert = () => {}

/**
 * The table as the stream sees it.
 *
 * Three of the five steps are the ordinary screens: once the answers are on the table
 * they are public, and a viewer should read them laid out exactly as the players do —
 * the snapshot behind them simply carries no hand, and every button they could offer is
 * already disabled by a `you` that wants nothing.
 *
 * The other two steps get a screen of their own, because the player's version of them is
 * made of things a spectator must not see: the salon is the host's control room, and the
 * writing step is where a hand would be. The game code is deliberately absent from both —
 * it is on screen in front of a whole chat, and it is the one thing that opens a seat.
 */
export function SpectatorBoard({ game, liveChatVotes }: Props) {
  switch (game.phase) {
    case 'LOBBY':
      return <WaitingRoom game={game} />
    case 'SUBMITTING':
      return <Writing game={game} />
    case 'SELECTING':
      if (game.settings.selectionFormat === 'DUELS') {
        return <DuelPanel game={game} onChoose={inert} liveChatVotes={liveChatVotes} />
      }
      return <VotePanel game={game} onChoose={inert} liveChatVotes={liveChatVotes} />
    case 'ROUND_RESULT':
      return <ResultPanel game={game} onNext={inert} />
    case 'FINISHED':
      return <FinishedPanel game={game} send={inert} />
  }
}

/** Before the first card: who is at the table, and what they are about to play. */
function WaitingRoom({ game }: { game: GameView }) {
  return (
    <Panel title="Salon">
      <p className="font-display text-lg text-ink/75">La partie n&apos;a pas encore commencé.</p>
      <p className="mt-1 text-sm text-ink/60">
        {game.players.length} joueur(s) à table · {game.settings.rounds} manches ·{' '}
        {judgeLabel(game)} tranche
      </p>
      <ul className="mt-4 flex flex-wrap gap-3">
        {game.players.map((player) => (
          <li key={player.id} className="sketch-alt flex items-center gap-2 bg-paper/70 px-3 py-2">
            <Avatar avatar={player.avatar} size={40} title={player.nickname} />
            <span className="font-display font-bold">{player.nickname}</span>
          </li>
        ))}
      </ul>
    </Panel>
  )
}

/**
 * The writing step. The situation is up — that is the half of the joke the viewers are
 * meant to be guessing against — and the answers are not, for anybody, not even the
 * players. All there is to show is who has finished.
 */
function Writing({ game }: { game: GameView }) {
  const round = game.round
  if (!round) return null
  const answering = game.players.filter((player) => !player.isCzar || game.settings.czarAnswers)
  const done = answering.filter((player) => player.hasAnswered)

  return (
    <RoundStage
      situation={
        <SituationCard
          card={round.situation}
          footer={`Manche ${round.number} · ${round.expectedAnswers} réponse${
            round.expectedAnswers > 1 ? 's' : ''
          }`}
        />
      }
    >
      <div className="flex flex-col items-center gap-4 py-6">
        <p className="text-center font-display text-lg text-ink/75">
          Les joueurs écrivent leur réponse…
        </p>
        <p className="font-display text-3xl font-black tabular-nums text-mint">
          {done.length} / {answering.length}
        </p>
        <ul className="flex flex-wrap justify-center gap-2">
          {answering.map((player) => (
            <li
              key={player.id}
              className={`sketch-pill flex items-center gap-2 px-3 py-1.5 text-sm font-semibold ${
                player.hasAnswered ? 'bg-mint/15 text-mint' : 'bg-paper/70 text-ink/60'
              }`}
            >
              <Avatar avatar={player.avatar} size={26} title={player.nickname} />
              {player.nickname}
            </li>
          ))}
        </ul>
      </div>
    </RoundStage>
  )
}

function judgeLabel(game: GameView): string {
  if (game.settings.selectionMode === 'CHAT') return 'le tchat'
  if (game.settings.selectionMode === 'CZAR') return 'le maître du jeu'
  return 'la table'
}
