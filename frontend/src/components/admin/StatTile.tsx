import { motion } from 'motion/react'

interface Props {
  label: string
  value: number | string
  accent?: string
}

/** One big number, because a dashboard is read at a glance. */
export function StatTile({ label, value, accent = 'text-honey' }: Props) {
  return (
    <div className="sketch bg-paper/70 p-5">
      <p className="text-xs font-semibold uppercase tracking-wider text-ink/60">{label}</p>
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
