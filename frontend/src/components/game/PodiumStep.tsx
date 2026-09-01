import { motion } from 'motion/react'
import type { PlayerView } from '../../api/types'
import { Avatar } from '../avatar/Avatar'

interface Props {
  player: PlayerView
  rank: 1 | 2 | 3
}

const MEDALS: Record<number, string> = { 1: '🥇', 2: '🥈', 3: '🥉' }
const BLOCK_HEIGHT: Record<number, string> = { 1: 'h-32', 2: 'h-24', 3: 'h-16' }
const BLOCK_TONE: Record<number, string> = {
  1: 'bg-linear-to-b from-zap/80 to-zap/30',
  2: 'bg-ink/10',
  3: 'bg-ink/5',
}

/** One step of the podium: the taller the block, the better the score. */
export function PodiumStep({ player, rank }: Props) {
  return (
    <motion.li
      initial={{ opacity: 0, y: 60 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: (3 - rank) * 0.18, type: 'spring', stiffness: 200, damping: 18 }}
      className="flex w-28 flex-col items-center gap-2 sm:w-36"
    >
      <span className="text-3xl" aria-hidden>
        {MEDALS[rank]}
      </span>
      <Avatar avatar={player.avatar} size={rank === 1 ? 104 : 80} title={player.nickname} />
      <p className="max-w-full truncate font-display text-lg font-bold">{player.nickname}</p>
      <div
        className={`flex w-full items-start justify-center rounded-t-2xl border-2 border-ink pt-2 ${BLOCK_HEIGHT[rank]} ${BLOCK_TONE[rank]}`}
      >
        <span className="font-display text-3xl font-black tabular-nums text-ink">{player.score}</span>
      </div>
    </motion.li>
  )
}
