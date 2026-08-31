import type { ButtonHTMLAttributes, ReactNode } from 'react'
import { motion } from 'motion/react'

type Variant = 'primary' | 'ghost' | 'danger' | 'zap'

interface Props extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'ref' | 'onAnimationStart' | 'onDragStart' | 'onDragEnd' | 'onDrag'> {
  variant?: Variant
  children: ReactNode
  full?: boolean
}

const VARIANTS: Record<Variant, string> = {
  primary: 'bg-punch text-white shadow-glow hover:brightness-110',
  zap: 'bg-zap text-ink hover:brightness-105',
  ghost: 'bg-white/10 text-white hover:bg-white/20',
  danger: 'bg-red-500/90 text-white hover:bg-red-500',
}

/** The one button of the app, springy on press so every tap feels answered. */
export function Button({ variant = 'primary', children, full = false, className = '', ...rest }: Props) {
  return (
    <motion.button
      whileHover={{ y: -2 }}
      whileTap={{ scale: 0.96 }}
      transition={{ type: 'spring', stiffness: 400, damping: 20 }}
      className={`inline-flex items-center justify-center gap-2 rounded-full px-6 py-3 font-display text-base font-bold transition disabled:cursor-not-allowed disabled:opacity-40 disabled:shadow-none ${
        VARIANTS[variant]
      } ${full ? 'w-full' : ''} ${className}`}
      {...rest}
    >
      {children}
    </motion.button>
  )
}
