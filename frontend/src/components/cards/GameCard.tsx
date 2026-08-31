import type { ReactNode } from 'react'
import { motion } from 'motion/react'
import { usePointerFine } from '../../hooks/usePointerFine'
import { useCardTilt } from './useCardTilt'

export type CardTone = 'situation' | 'punchline'

/**
 * `portrait` keeps the proportions of a real card, used for a hand where every card is
 * the same size. `flexible` lets the card grow with its text, which is what stops long
 * answers from being clipped in a grid.
 */
export type CardShape = 'portrait' | 'flexible'

interface Props {
  tone: CardTone
  children: ReactNode
  footer?: ReactNode
  onClick?: () => void
  onHover?: () => void
  selected?: boolean
  disabled?: boolean
  shape?: CardShape
  className?: string
  ariaLabel?: string
}

const TONES: Record<CardTone, string> = {
  situation: 'bg-ink-soft text-paper ring-white/15',
  punchline: 'bg-paper text-ink ring-black/10',
}

const SHAPES: Record<CardShape, string> = {
  portrait: 'aspect-5/7',
  flexible: 'h-full min-h-44',
}

/**
 * The shell every card shares: paper texture, rounded corners, and a 3D tilt that
 * follows the pointer. Selecting a card lifts it out of the row.
 */
export function GameCard({
  tone,
  children,
  footer,
  onClick,
  onHover,
  selected = false,
  disabled = false,
  shape = 'portrait',
  className = '',
  ariaLabel,
}: Props) {
  const tilt = useCardTilt()
  const interactive = Boolean(onClick) && !disabled
  const canTilt = usePointerFine()

  return (
    <div className="card-stage h-full">
      <motion.div
        role={interactive ? 'button' : undefined}
        tabIndex={interactive ? 0 : undefined}
        aria-pressed={interactive ? selected : undefined}
        aria-label={ariaLabel}
        onClick={interactive ? onClick : undefined}
        onFocus={onHover}
        onKeyDown={(event) => {
          if (!interactive) return
          if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault()
            onClick?.()
          }
        }}
        onPointerMove={canTilt ? tilt.onPointerMove : undefined}
        onPointerEnter={onHover}
        onPointerLeave={canTilt ? tilt.onPointerLeave : undefined}
        style={canTilt ? { rotateX: tilt.rotateX, rotateY: tilt.rotateY, transformStyle: 'preserve-3d' } : undefined}
        animate={{ y: selected ? -18 : 0, scale: selected ? 1.04 : 1 }}
        whileHover={interactive ? { y: selected ? -22 : -10 } : undefined}
        whileTap={interactive ? { scale: 0.97 } : undefined}
        transition={{ type: 'spring', stiffness: 320, damping: 24 }}
        className={`card-fit relative flex w-full flex-col justify-between overflow-hidden rounded-3xl p-4 shadow-card ring-1 transition-shadow sm:p-5 ${
          SHAPES[shape]
        } ${TONES[tone]} ${interactive ? 'cursor-pointer select-none' : ''} ${
          disabled ? 'opacity-45 grayscale' : ''
        } ${selected ? 'shadow-glow' : ''} ${className}`}
      >
        {canTilt && (
          <motion.span
            aria-hidden
            className="pointer-events-none absolute inset-0 opacity-45 mix-blend-soft-light"
            style={{
              background: `radial-gradient(circle at ${tilt.glareX} ${tilt.glareY}, rgba(255,255,255,0.9), transparent 55%)`,
            }}
          />
        )}
        <div className="relative z-10 flex-1">{children}</div>
        {footer && <div className="relative z-10 mt-4 text-xs font-semibold opacity-60">{footer}</div>}
      </motion.div>
    </div>
  )
}
