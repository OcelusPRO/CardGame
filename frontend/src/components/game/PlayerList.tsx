import type { GameView } from '../../api/types'
import { Panel } from '../ui/Panel'
import { PlayerRow } from './PlayerRow'

interface Props {
  game: GameView
  onKick?: (playerId: string) => void
}

/** The scoreboard and the waiting list, which are the same thing in this game. */
export function PlayerList({ game, onKick }: Props) {
  const ordered = [...game.players].sort((a, b) => b.score - a.score)
  const canKick = game.you.isHost && game.phase === 'LOBBY'

  return (
    <Panel title={`Joueurs (${game.players.length}/${game.settings.maxPlayers})`}>
      <ul className="flex flex-col gap-2">
        {ordered.map((player) => (
          <PlayerRow
            key={player.id}
            player={player}
            phase={game.phase}
            isYou={player.id === game.you.id}
            onKick={canKick && player.id !== game.you.id && onKick ? () => onKick(player.id) : undefined}
          />
        ))}
      </ul>
      <p className="mt-3 text-xs text-white/40">
        Manche {game.round?.number ?? 0} / {game.settings.rounds} · le meilleur score gagne
      </p>
    </Panel>
  )
}
