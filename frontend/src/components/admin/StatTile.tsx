import { motion } from 'motion/react'

interface Props {
  label: string
  value: number | string
  accent?: string
}

/** One big number, because a dashboard is read at a glance. */
export function StatTile({ label, value, accent = 'text-zap' }: Props) {
  return (
    <div className="rounded-3xl bg-white/5 p-5 ring-1 ring-white/10">
      <p className="text-xs font-semibold uppercase tracking-wider text-white/50">{label}</p>
      <motion.p
        key={String(value)}
        initial={{ scale: 0.9, opacity: 0.4 }}
        animate={{ scale: 1, opacity: 1 }}
        className={`mt-1 font-display text-4xl font-black tabular-nums ${accent}`}
      >
        {value}
      </motion.p>
    </div>
  )
}
