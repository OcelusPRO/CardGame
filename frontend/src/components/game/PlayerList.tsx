import type { GameView } from '../../api/types'
import { Panel } from '../ui/Panel'
import { PlayerRow } from './PlayerRow'

interface Props {
  game: GameView
  onKick?: (playerId: string) => void
  onLeave?: () => void
}

/** The scoreboard and the waiting list, which are the same thing in this game. */
export function PlayerList({ game, onKick, onLeave }: Props) {
  const ordered = [...game.players].sort((a, b) => b.score - a.score)
  const canKick = game.you.isHost && game.phase === 'LOBBY'
  // On a shared device nobody is "you": the phone changes hands. Leaving is the device's
  // own business — it takes the whole sofa with it — so it hangs on the host's row, and
  // only while the screen faces the table rather than somebody's hand.
  const self = game.sharedDevice ? undefined : game.you.id
  const leaver = game.sharedDevice ? (game.seat ? undefined : game.you.id) : game.you.id

  return (
    <Panel title={`Joueurs (${game.players.length}/${game.settings.maxPlayers})`}>
      <ul className="flex flex-col gap-2">
        {ordered.map((player) => (
          <PlayerRow
            key={player.id}
            player={player}
            phase={game.phase}
            isYou={player.id === self}
            onKick={canKick && player.id !== game.you.id && onKick ? () => onKick(player.id) : undefined}
            onLeave={player.id === leaver ? onLeave : undefined}
          />
        ))}
      </ul>
      <p className="mt-3 text-xs text-ink/50">
        Manche {game.round?.number ?? 0} / {game.settings.rounds} · le meilleur score gagne
      </p>
    </Panel>
  )
}
