import { motion } from 'motion/react'
import type { GamePhase, PlayerView } from '../../api/types'
import { Avatar } from '../avatar/Avatar'

interface Props {
  player: PlayerView
  phase: GamePhase
  isYou: boolean
  onKick?: () => void
}

/** One seat at the table: who they are, where they stand, and what we wait for. */
export function PlayerRow({ player, phase, isYou, onKick }: Props) {
  return (
    <motion.li
      layout
      initial={{ opacity: 0, x: -12 }}
      animate={{ opacity: 1, x: 0 }}
      className={`flex items-center gap-3 rounded-2xl px-3 py-2 ${
        player.connected ? 'bg-white/5' : 'bg-white/5 opacity-45'
      }`}
    >
      <Avatar avatar={player.avatar} size={52} title={player.nickname} />
      <div className="min-w-0 flex-1">
        <p className="truncate font-display font-bold">
          {player.nickname}
          {isYou && <span className="ml-1 text-xs font-semibold text-mint">(vous)</span>}
        </p>
        <p className="text-xs text-white/50">{statusOf(player, phase)}</p>
      </div>
      <span className="font-display text-xl font-bold tabular-nums text-zap">{player.score}</span>
      {onKick && (
        <button
          type="button"
          onClick={onKick}
          aria-label={`Exclure ${player.nickname}`}
          className="rounded-full px-2 py-1 text-sm text-white/40 transition hover:bg-red-500/20 hover:text-red-300"
        >
          ✕
        </button>
      )}
    </motion.li>
  )
}

function statusOf(player: PlayerView, phase: GamePhase): string {
  if (!player.connected) return 'Déconnecté'
  if (player.isHost) return player.isCzar ? 'Hôte · maître du jeu' : 'Hôte'
  if (player.isCzar) return 'Maître du jeu'
  if (phase === 'SUBMITTING') return player.hasAnswered ? 'A joué' : 'Réfléchit…'
  if (phase === 'SELECTING') return player.hasVoted ? 'A voté' : 'Hésite…'
  return 'En jeu'
}
