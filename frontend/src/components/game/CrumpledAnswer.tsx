import { useState, type ReactNode } from 'react'
import { motion, useReducedMotion } from 'motion/react'

interface Props {
  children: ReactNode
  /** Staggers the wave, so the losing answers do not all ball up on the same frame. */
  delay: number
}

const DURATION = 2.4

/**
 * The fate of an answer nobody picked: it is read for a beat, screwed up into a ball, and
 * thrown off the bottom of the screen.
 *
 * The paper folds are a gradient overlay fading in while the card shrinks, and the card
 * rounds off as it goes so the last thing on screen reads as a ball rather than a small
 * card. Once it has left, the node is dropped and the surviving cards close the gap on
 * their own — the grid animates its layout.
 */
export function CrumpledAnswer({ children, delay }: Props) {
  const reducedMotion = useReducedMotion()
  const [gone, setGone] = useState(false)
  // Measured once, at mount: the card has to clear the tallest viewport it might be on.
  const [fallDistance] = useState(() =>
    typeof window === 'undefined' ? 900 : window.innerHeight * 1.15,
  )

  // Somebody who asked for calm gets the plain card, kept on the table.
  if (reducedMotion) return <>{children}</>
  if (gone) return null

  return (
    <motion.div
      aria-hidden={false}
      className="relative h-full"
      style={{ transformOrigin: '50% 55%', overflow: 'hidden' }}
      animate={{
        scale: [1, 1.02, 0.52, 0.38, 0.34],
        rotate: [0, -2, 9, -7, 34],
        x: [0, 0, 4, -6, 26],
        y: [0, -8, 6, 14, fallDistance],
        borderRadius: ['18px', '18px', '38%', '48%', '48%'],
        opacity: [1, 1, 1, 1, 0.25],
      }}
      transition={{
        duration: DURATION,
        delay,
        times: [0, 0.14, 0.36, 0.5, 1],
        ease: ['easeOut', 'easeIn', 'easeOut', 'easeIn'],
      }}
      onAnimationComplete={() => setGone(true)}
    >
      {children}
      <motion.span
        aria-hidden
        className="crumple-creases pointer-events-none absolute inset-0"
        initial={{ opacity: 0 }}
        animate={{ opacity: [0, 0, 0.95, 0.95, 0.95] }}
        transition={{ duration: DURATION, delay, times: [0, 0.14, 0.36, 0.5, 1] }}
      />
    </motion.div>
  )
}
