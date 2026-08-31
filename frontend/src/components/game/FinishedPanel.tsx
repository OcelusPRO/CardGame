import { Link } from 'react-router-dom'
import type { GameView, PlayerView } from '../../api/types'
import type { ClientMessage } from '../../game/messages'
import { messages } from '../../game/messages'
import { Avatar } from '../avatar/Avatar'
import { Button } from '../ui/Button'
import { Panel } from '../ui/Panel'
import { PodiumStep } from './PodiumStep'

interface Props {
  game: GameView
  send: (message: ClientMessage) => void
}

/** A real podium for the top three, then a plain ranking for everybody else. */
export function FinishedPanel({ game, send }: Props) {
  const ranked = [...game.players].sort((a, b) => b.score - a.score)
  const podium = ranked.slice(0, 3)
  const rest = ranked.slice(3)

  // Second on the left, first in the middle, third on the right, like a real one.
  const arrangement: { player: PlayerView; rank: 1 | 2 | 3 }[] = [
    podium[1] && { player: podium[1], rank: 2 as const },
    podium[0] && { player: podium[0], rank: 1 as const },
    podium[2] && { player: podium[2], rank: 3 as const },
  ].filter(Boolean) as { player: PlayerView; rank: 1 | 2 | 3 }[]

  return (
    <Panel title="Fin de la partie">
      <ol className="flex items-end justify-center gap-3 sm:gap-6">
        {arrangement.map(({ player, rank }) => (
          <PodiumStep key={player.id} player={player} rank={rank} />
        ))}
      </ol>

      {rest.length > 0 && (
        <ol className="mx-auto mt-6 flex max-w-md flex-col gap-1">
          {rest.map((player, index) => (
            <li
              key={player.id}
              className="flex items-center gap-3 rounded-2xl bg-white/5 px-3 py-2 text-sm"
            >
              <span className="w-6 text-center font-display font-bold text-white/40">{index + 4}</span>
              <Avatar avatar={player.avatar} size={36} title={player.nickname} />
              <span className="min-w-0 flex-1 truncate font-semibold">{player.nickname}</span>
              <span className="font-display text-lg font-bold tabular-nums text-zap">{player.score}</span>
            </li>
          ))}
        </ol>
      )}

      <div className="mt-6 flex flex-col items-center gap-2">
        {game.you.isHost ? (
          <Button onClick={() => send(messages.lobby())}>Retour au salon 🔄</Button>
        ) : (
          <>
            <p className="text-sm text-white/55">
              L&apos;hôte peut relancer une partie depuis le salon.
            </p>
            <Link to="/">
              <Button variant="ghost">Retour à l&apos;accueil</Button>
            </Link>
          </>
        )}
      </div>
    </Panel>
  )
}
