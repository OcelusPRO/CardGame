import { motion } from 'motion/react'
import { useCountdown } from '../../hooks/useCountdown'

interface Props {
  deadlineMillis?: number
  serverTimeMillis: number
  totalSeconds: number
  label: string
}

/** A shrinking bar plus the seconds left, so the pressure is visible without a glance. */
export function PhaseTimer({ deadlineMillis, serverTimeMillis, totalSeconds, label }: Props) {
  const remaining = useCountdown(deadlineMillis, serverTimeMillis)
  if (remaining === null) return null

  const ratio = Math.max(0, Math.min(1, remaining / totalSeconds))
  const urgent = remaining <= 10

  return (
    <div className="flex items-center gap-3">
      <div className="h-2 flex-1 overflow-hidden rounded-full bg-white/10">
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
          urgent ? 'text-punch' : 'text-white/70'
        }`}
      >
        {remaining}
      </motion.span>
    </div>
  )
}
