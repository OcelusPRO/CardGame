import { useEffect, useRef, useState, type ReactNode } from 'react'
import { useReducedMotionConfig } from 'motion/react'
import { drawCardFace, type CardFace } from './crumple/cardTexture'
import { crumpleCard } from './crumple/crumpleStage'

interface Props {
  children: ReactNode
  /** What the 3D sheet has to be painted with, since it stands in for the real card. */
  face: CardFace
  /** Staggers the wave, so the losing answers do not all ball up on the same frame. */
  delay: number
}

/**
 * The fate of an answer nobody picked: it is read for a beat, screwed up into a ball, and
 * thrown off the bottom of the screen.
 *
 * The card is not animated in place — a rectangle of HTML has no way to fold. When its
 * turn comes, its screen rectangle is measured, its face is painted onto a canvas, and a
 * real sheet of geometry takes over at exactly the same spot: a subdivided plane wearing
 * that painting as its skin, whose vertices are then pulled onto a lumpy sphere. The
 * paper genuinely folds, and the flat shading gives every facet its own light.
 *
 * The DOM card stays mounted, invisible, while its sheet flies. Removing it there would
 * let the grid close the gap under a ball that has not landed yet; the node goes once the
 * throw is over, and the surviving cards slide together then.
 */
export function CrumpledAnswer({ children, face, delay }: Props) {
  // The config hook, not `useReducedMotion`: the latter only ever reads the OS switch,
  // so a player whose system asks for less motion would never get the crumple back after
  // turning animations on in the header — and one who turned them off would still get it.
  const reducedMotion = useReducedMotionConfig()
  const [thrown, setThrown] = useState(false)
  const [gone, setGone] = useState(false)
  const holder = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (reducedMotion) return
    let cancelled = false

    const timer = setTimeout(() => {
      const node = holder.current
      if (!node) return

      const rect = node.getBoundingClientRect()
      if (rect.width < 1 || rect.height < 1) {
        setGone(true)
        return
      }

      const scale = Math.min(window.devicePixelRatio || 1, 2)
      const painted = drawCardFace(face, rect.width, rect.height, scale)

      // Hidden only once the sheet has been handed over, so the two never overlap.
      setThrown(true)
      crumpleCard({ rect, face: painted }).then(() => {
        if (!cancelled) setGone(true)
      })
    }, delay * 1000)

    return () => {
      cancelled = true
      clearTimeout(timer)
    }
  }, [delay, face, reducedMotion])

  // Somebody who asked for calm gets the plain card, kept on the table. A sheet already
  // in the air keeps its flight, though: silencing animations mid-throw must not drop a
  // vanished card back onto the table.
  if (gone) return null
  if (reducedMotion && !thrown) return <>{children}</>

  return (
    <div ref={holder} className="h-full" style={{ visibility: thrown ? 'hidden' : 'visible' }}>
      {children}
    </div>
  )
}
