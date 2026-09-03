import { useEffect, useState } from 'react'
import { AnimatePresence, motion, useReducedMotionConfig } from 'motion/react'
import type { PunchlineCardView } from '../../api/types'
import { CardBack } from './CardBack'
import { PunchlineCard } from './PunchlineCard'

interface Props {
  cards: PunchlineCardView[]
  selected: string[]
  onToggle: (cardId: string) => void
  disabled?: boolean
  /** Words typed into each card's holes, keyed by card id. */
  fills?: Record<string, string[]>
}

/**
 * The hand. A scroll-snap row on a phone, a grid that uses the whole width on a wide
 * screen, so no card ever ends up squeezed to the point of chopping words in half.
 *
 * The first time a hand appears — when the game starts — the cards are dealt: they slide
 * in face down from off the left edge, each a beat behind the last, and turn over once
 * they reach their place. Later hands just settle in, and with motion switched off the
 * whole flight is skipped.
 */
export function CardHand({ cards, selected, onToggle, disabled = false, fills = {} }: Props) {
  const still = useReducedMotionConfig()
  const [dealing, setDealing] = useState(!still)

  useEffect(() => {
    if (!dealing) return
    const done = window.setTimeout(() => setDealing(false), 1100 + cards.length * 120 + 900)
    return () => window.clearTimeout(done)
    // Only ever the opening deal: the hand length at mount is all this needs.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return (
    <div
      className={`-mx-4 px-4 pb-6 pt-8 sm:mx-0 sm:px-0 ${
        dealing ? 'overflow-x-clip' : 'overflow-x-auto sm:overflow-visible'
      }`}
    >
      <div
        className="flex min-w-max gap-3 sm:grid sm:min-w-0 sm:grid-cols-[repeat(auto-fit,minmax(11rem,1fr))] sm:gap-4"
        style={dealing ? { perspective: '1600px' } : undefined}
      >
        <AnimatePresence initial={dealing}>
          {cards.map((card, index) => {
            const order = selected.indexOf(card.id)
            return (
              <HandCard
                key={card.id}
                deal={dealing}
                index={index}
                total={cards.length}
              >
                <PunchlineCard
                  card={card}
                  selected={order >= 0}
                  order={order >= 0 ? order + 1 : undefined}
                  disabled={disabled}
                  fills={fills[card.id]}
                  onClick={disabled ? undefined : () => onToggle(card.id)}
                />
              </HandCard>
            )
          })}
        </AnimatePresence>
      </div>
    </div>
  )
}

/**
 * A single card in the hand. Dealt on the opening hand — flown in from off the edge
 * showing the deck's back, then cross-faded to its face at the moment it lands, which is
 * timed to the card being edge-on so the turn reads as one motion whatever the browser
 * makes of the 3D. Every hand after that, it just springs into place.
 */
function HandCard({
  deal,
  index,
  total,
  children,
}: {
  deal: boolean
  index: number
  total: number
  children: React.ReactNode
}) {
  const [faceUp, setFaceUp] = useState(!deal)
  const [showBack, setShowBack] = useState(deal)
  const flipAt = index * 0.12 + 0.62

  useEffect(() => {
    if (!deal) return
    const turn = window.setTimeout(() => setFaceUp(true), (flipAt + 0.16) * 1000)
    const clear = window.setTimeout(() => setShowBack(false), (flipAt + 0.6) * 1000)
    return () => {
      window.clearTimeout(turn)
      window.clearTimeout(clear)
    }
  }, [deal, flipAt])

  if (!deal) {
    return (
      <motion.div
        layout
        initial={{ opacity: 0, y: 40, rotate: 0 }}
        animate={{ opacity: 1, y: 0, rotate: fan(index, total) }}
        exit={{ opacity: 0, y: -60, scale: 0.8 }}
        transition={{ type: 'spring', stiffness: 260, damping: 24 }}
        className="w-44 shrink-0 sm:w-auto"
      >
        {children}
      </motion.div>
    )
  }

  return (
    <motion.div
      initial={{ opacity: 0, x: '-78vw', y: 70, rotate: -18, rotateY: 180, scale: 0.85 }}
      animate={{ opacity: 1, x: 0, y: 0, rotate: fan(index, total), rotateY: 0, scale: 1 }}
      exit={{ opacity: 0, y: -60, scale: 0.8 }}
      transition={{
        type: 'spring',
        stiffness: 150,
        damping: 24,
        delay: index * 0.12,
        opacity: { duration: 0.18, delay: index * 0.12 },
        rotateY: { type: 'spring', stiffness: 170, damping: 20, delay: flipAt },
      }}
      style={{ transformStyle: 'preserve-3d' }}
      className="relative w-44 shrink-0 sm:w-auto"
    >
      {showBack && (
        <motion.div
          aria-hidden="true"
          className="pointer-events-none absolute inset-0 [backface-visibility:hidden] [transform:rotateY(180deg)]"
          initial={{ opacity: 1 }}
          animate={{ opacity: faceUp ? 0 : 1 }}
          transition={{ duration: 0.12 }}
        >
          <CardBack />
        </motion.div>
      )}
      <motion.div
        className="[backface-visibility:hidden]"
        initial={{ opacity: 0 }}
        animate={{ opacity: faceUp ? 1 : 0 }}
        transition={{ duration: 0.12 }}
      >
        {children}
      </motion.div>
    </motion.div>
  )
}

/** A gentle arc on the scrolling row: the outer cards lean away, the middle stays upright. */
function fan(index: number, total: number): number {
  if (total < 2) return 0
  const middle = (total - 1) / 2
  return ((index - middle) / middle) * 2
}
