import type { PointerEvent } from 'react'
import { useMotionValue, useSpring, useTransform } from 'motion/react'

const SPRING = { stiffness: 260, damping: 22, mass: 0.6 }

/**
 * Follows the pointer over a card and turns it into a rotation plus a moving glare,
 * which is what makes the card feel held rather than printed on the page.
 */
export function useCardTilt(strength = 14) {
  const pointerX = useMotionValue(0.5)
  const pointerY = useMotionValue(0.5)

  const rotateX = useSpring(useTransform(pointerY, [0, 1], [strength, -strength]), SPRING)
  const rotateY = useSpring(useTransform(pointerX, [0, 1], [-strength, strength]), SPRING)
  const glareX = useTransform(pointerX, (value) => `${value * 100}%`)
  const glareY = useTransform(pointerY, (value) => `${value * 100}%`)

  const onPointerMove = (event: PointerEvent<HTMLElement>) => {
    const bounds = event.currentTarget.getBoundingClientRect()
    pointerX.set((event.clientX - bounds.left) / bounds.width)
    pointerY.set((event.clientY - bounds.top) / bounds.height)
  }

  const onPointerLeave = () => {
    pointerX.set(0.5)
    pointerY.set(0.5)
  }

  return { rotateX, rotateY, glareX, glareY, onPointerMove, onPointerLeave }
}
