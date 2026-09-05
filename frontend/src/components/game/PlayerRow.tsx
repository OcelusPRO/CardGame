import { useEffect, useState } from 'react'
import { motion } from 'motion/react'
import type { GamePhase, PlayerView } from '../../api/types'
import { Avatar } from '../avatar/Avatar'

interface Props {
  player: PlayerView
  phase: GamePhase
  isYou: boolean
  onKick?: () => void
  onLeave?: () => void
}

/** One seat at the table: who they are, where they stand, and what we wait for. */
export function PlayerRow({ player, phase, isYou, onKick, onLeave }: Props) {
  return (
    <motion.li
      layout
      initial={{ opacity: 0, x: -12 }}
      animate={{ opacity: 1, x: 0 }}
      className={`sketch-alt flex items-center gap-3 px-3 py-2 ${
        player.connected ? 'bg-paper/70' : 'bg-paper/70 opacity-45'
      }`}
    >
      <Avatar avatar={player.avatar} size={52} title={player.nickname} />
      <div className="min-w-0 flex-1">
        <p className="truncate font-display font-bold">
          {player.nickname}
          {player.isHost && <HostCrown />}
          {isYou && <span className="ml-1 text-xs font-semibold text-mint">(vous)</span>}
        </p>
        <p className="text-xs text-ink/60">{statusOf(player, phase)}</p>
      </div>
      <span className="font-display text-xl font-bold tabular-nums text-honey">{player.score}</span>
      {onLeave && <LeaveSelfButton onLeave={onLeave} />}
      {onKick && (
        <button
          type="button"
          onClick={onKick}
          aria-label={`Exclure ${player.nickname}`}
          className="rounded-full px-2 py-1 text-sm text-ink/50 transition hover:bg-red-500/20 hover:text-red-300"
        >
          ✕
        </button>
      )}
    </motion.li>
  )
}

/**
 * Leaving your own seat, worn like the host's kick button but pointed at yourself. One tap
 * arms it, a second within a few seconds confirms — a stray thumb never drops you mid-game.
 */
function LeaveSelfButton({ onLeave }: { onLeave: () => void }) {
  const [armed, setArmed] = useState(false)

  useEffect(() => {
    if (!armed) return
    const timer = setTimeout(() => setArmed(false), 3000)
    return () => clearTimeout(timer)
  }, [armed])

  return (
    <button
      type="button"
      onClick={() => (armed ? onLeave() : setArmed(true))}
      aria-label={armed ? 'Confirmer et quitter la partie' : 'Quitter la partie'}
      className={
        armed
          ? 'shrink-0 rounded-full bg-red-500/20 px-2 py-1 text-xs font-bold text-red-300'
          : 'shrink-0 rounded-full px-2 py-1 text-sm text-ink/50 transition hover:bg-red-500/20 hover:text-red-300'
      }
    >
      {armed ? 'Quitter ?' : '🚪'}
    </button>
  )
}

/** A rough, hand-drawn crown marking the host, so the status line is free to say what
 *  they are doing this round like everyone else. */
function HostCrown() {
  return (
    <span
      className="ml-1 inline-flex align-[-0.15em]"
      title="Hôte"
      role="img"
      aria-label="Hôte"
    >
      <svg
        viewBox="0 0 24 20"
        className="h-[1em] w-[1.2em] text-honey"
        fill="currentColor"
        fillOpacity={0.18}
        stroke="currentColor"
        strokeWidth={1.7}
        strokeLinecap="round"
        strokeLinejoin="round"
        aria-hidden
      >
        <path d="M2.6 15.2 L1.9 6.3 L7.8 10.8 L11.8 3.2 L16.3 10.9 L22 6 L20.7 15.5 C20.4 16 15.9 16.7 11.7 16.6 C7.4 16.5 3 16 2.6 15.2 Z" />
        <path d="M3.4 18.4 C7 17.4 17 17.5 20.4 18.3" fill="none" />
      </svg>
    </span>
  )
}

function statusOf(player: PlayerView, phase: GamePhase): string {
  if (!player.connected) return 'Déconnecté'
  if (player.isCzar) return 'Maître du jeu'
  if (phase === 'LOBBY') return 'En attente'
  if (phase === 'SUBMITTING') return player.hasAnswered ? 'A joué' : 'Réfléchit…'
  if (phase === 'SELECTING') return player.hasVoted ? 'A voté' : 'Hésite…'
  return 'En jeu'
}
