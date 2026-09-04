import { motion } from 'motion/react'
import { useTimerSound } from '../../audio/useGameSounds'
import { useCountdown } from '../../hooks/useCountdown'

interface Props {
  deadlineMillis?: number
  serverTimeMillis: number
  totalSeconds: number
  label: string
  /** Whether running out of time is worth hearing. A result screen simply moves on. */
  chime?: boolean
}

/** A shrinking bar plus the seconds left, so the pressure is visible without a glance. */
export function PhaseTimer({ deadlineMillis, serverTimeMillis, totalSeconds, label, chime = true }: Props) {
  const remaining = useCountdown(deadlineMillis, serverTimeMillis)
  useTimerSound(remaining, chime)
  if (remaining === null) return null

  const ratio = Math.max(0, Math.min(1, remaining / totalSeconds))
  const urgent = remaining <= 10

  return (
    <div className="flex items-center gap-3">
      <div className="sketch-pill h-3 flex-1 overflow-hidden bg-paper">
        <motion.div
          className={`h-full rounded-full ${urgent ? 'bg-punch' : 'bg-mint'}`}
          animate={{ width: `${ratio * 100}%` }}
          transition={{ ease: 'linear', duration: 0.25 }}
        />
      </div>
      <motion.span
        aria-label={`${label} : ${remaining} secondes`}
        animate={urgent ? { scale: [1, 1.18, 1] } : { scale: 1 }}
        transition={{ duration: 0.6, repeat: urgent ? Infinity : 0 }}
        className={`w-10 text-right font-display text-lg font-bold tabular-nums ${
          urgent ? 'text-punch' : 'text-ink/75'
        }`}
      >
        {remaining}
      </motion.span>
    </div>
  )
}
