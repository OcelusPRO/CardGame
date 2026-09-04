import type { ButtonHTMLAttributes, ReactNode } from 'react'
import { motion } from 'motion/react'
import { playSound } from '../../audio/engine'
import type { SoundName } from '../../audio/sounds'

type Variant = 'primary' | 'ghost' | 'danger' | 'zap'

interface Props extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'ref' | 'onAnimationStart' | 'onDragStart' | 'onDragEnd' | 'onDrag'> {
  variant?: Variant
  children: ReactNode
  full?: boolean
  /** The effect played on press. `null` for a button that has its own sound. */
  sound?: SoundName | null
}

const VARIANTS: Record<Variant, string> = {
  primary: 'bg-punch text-paper hover:brightness-105',
  zap: 'bg-zap text-ink hover:brightness-105',
  ghost: 'bg-paper text-ink hover:bg-ink/8',
  danger: 'bg-red-500 text-paper hover:brightness-105',
}

/** The one button of the app, springy on press so every tap feels answered. */
export function Button({
  variant = 'primary',
  children,
  full = false,
  className = '',
  sound = 'click',
  onClick,
  ...rest
}: Props) {
  return (
    <motion.button
      whileHover={{ y: -2 }}
      whileTap={{ scale: 0.96 }}
      transition={{ type: 'spring', stiffness: 400, damping: 20 }}
      onClick={(event) => {
        if (sound) playSound(sound)
        onClick?.(event)
      }}
      className={`sketch-pill inline-flex items-center justify-center gap-2 px-6 py-3 font-display text-base font-bold shadow-card transition disabled:cursor-not-allowed disabled:opacity-40 disabled:shadow-none ${
        VARIANTS[variant]
      } ${full ? 'w-full' : ''} ${className}`}
      {...rest}
    >
      {children}
    </motion.button>
  )
}
