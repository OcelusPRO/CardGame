import type { GameView } from '../../api/types'
import { Button } from '../ui/Button'

interface Props {
  game: GameView
  onStart: () => void
}

/**
 * The one action the host is waiting to take. Pinned to the bottom of a phone and to the
 * side column on a wide screen, so it never scrolls away behind the deck editor.
 */
export function StartGameBar({ game, onStart }: Props) {
  const connected = game.players.filter((player) => player.connected).length
  const missing = Math.max(0, game.settings.minPlayers - connected)

  if (!game.you.isHost) {
    return (
      <p className="sketch bg-paper/70 px-4 py-3 text-center text-sm text-ink/65">
        L&apos;hôte lance la partie quand tout le monde est prêt.
      </p>
    )
  }

  return (
    <div className="flex flex-col gap-2">
      <Button full onClick={onStart} disabled={missing > 0}>
        {missing > 0 ? `Encore ${missing} joueur${missing > 1 ? 's' : ''}…` : 'Lancer la partie 🚀'}
      </Button>
      <p className="text-center text-xs text-ink/55">
        {game.settings.rounds} manches · {game.deck.situationsLeft} situations dans le paquet
      </p>
    </div>
  )
}
