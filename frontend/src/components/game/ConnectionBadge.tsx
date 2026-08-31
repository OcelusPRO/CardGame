import type { SocketStatus } from '../../game/GameSocket'

interface Props {
  status: SocketStatus
}

const LABELS: Record<SocketStatus, { text: string; className: string }> = {
  open: { text: 'En ligne', className: 'bg-mint/15 text-mint' },
  connecting: { text: 'Connexion…', className: 'bg-zap/15 text-zap animate-pulse' },
  closed: { text: 'Hors ligne', className: 'bg-red-500/15 text-red-300' },
}

/** Tells the player whether the table is still listening. */
export function ConnectionBadge({ status }: Props) {
  const { text, className } = LABELS[status]
  return (
    <span className={`rounded-full px-3 py-1 text-xs font-bold ${className}`}>{text}</span>
  )
}
