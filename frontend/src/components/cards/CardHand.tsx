import { useCallback, useEffect, useRef, useState } from 'react'
import { AnimatePresence, motion, useReducedMotionConfig } from 'motion/react'
import type { PunchlineCardView } from '../../api/types'
import { playSound } from '../../audio/engine'
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
 * Cards are dealt: they fly in face down from the bottom-left corner, a beat apart, and
 * turn over as they land. Only cards that have never been on the table are dealt — the
 * opening hand all at once, then just the fresh draw at the top of each round, while the
 * cards already held stay exactly where they are. With motion switched off, cards appear.
 */
export function CardHand({ cards, selected, onToggle, disabled = false, fills = {} }: Props) {
  const still = useReducedMotionConfig()
  const seen = useRef<Set<string>>(new Set())
  const flying = useRef(0)
  const [dealing, setDealing] = useState(false)

  // Card ids on the table now that we have never rendered before: this round's newcomers.
  const batch = still ? [] : cards.filter((card) => !seen.current.has(card.id)).map((card) => card.id)

  useEffect(() => {
    cards.forEach((card) => seen.current.add(card.id))
  })

  const onFlyStart = useCallback(() => {
    flying.current += 1
    setDealing(true)
  }, [])

  const onFlyEnd = useCallback(() => {
    flying.current = Math.max(0, flying.current - 1)
    if (flying.current === 0) setDealing(false)
  }, [])

  // While anything is in flight the box has to clip: a card starts a screen-width away.
  const clipping = dealing || batch.length > 0

  return (
    <div
      className={`-mx-4 px-4 pb-6 pt-8 sm:mx-0 sm:px-0 ${
        clipping ? 'overflow-clip' : 'overflow-x-auto sm:overflow-visible'
      }`}
    >
      <div
        className="flex min-w-max gap-3 sm:grid sm:min-w-0 sm:grid-cols-[repeat(auto-fit,minmax(11rem,1fr))] sm:gap-4"
        style={clipping ? { perspective: '1600px' } : undefined}
      >
        <AnimatePresence>
          {cards.map((card, index) => {
            const order = selected.indexOf(card.id)
            return (
              <HandCard
                key={card.id}
                dealSlot={batch.indexOf(card.id)}
                index={index}
                total={cards.length}
                onFlyStart={onFlyStart}
                onFlyEnd={onFlyEnd}
              >
                <PunchlineCard
                  card={card}
                  selected={order >= 0}
                  order={order >= 0 ? order + 1 : undefined}
                  disabled={disabled}
                  fills={fills[card.id]}
                  onClick={
                    disabled
                      ? undefined
                      : () => {
                          playSound(order >= 0 ? 'cardDeselect' : 'cardSelect')
                          onToggle(card.id)
                        }
                  }
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
 * One card in the hand. A card fresh off the deck (`dealSlot >= 0`) flies in from the
 * bottom-left corner showing the deck's back, then cross-fades to its face at the moment
 * it lands — timed to the card being edge-on so the turn reads as one motion whatever the
 * browser makes of the 3D. Once it has landed, and for every card already held, it is
 * just a plain card sitting in the grid.
 */
function HandCard({
  dealSlot,
  index,
  total,
  onFlyStart,
  onFlyEnd,
  children,
}: {
  dealSlot: number
  index: number
  total: number
  onFlyStart: () => void
  onFlyEnd: () => void
  children: React.ReactNode
}) {
  const [slot] = useState(dealSlot)
  const deal = slot >= 0
  const [faceUp, setFaceUp] = useState(!deal)
  const [showBack, setShowBack] = useState(deal)
  const [settled, setSettled] = useState(!deal)
  const flipAt = slot * 0.09 + 0.5

  useEffect(() => {
    if (!deal) return
    onFlyStart()
    const turn = window.setTimeout(() => setFaceUp(true), (flipAt + 0.16) * 1000)
    const land = window.setTimeout(() => {
      setShowBack(false)
      setSettled(true)
      onFlyEnd()
    }, (flipAt + 0.62) * 1000)
    return () => {
      window.clearTimeout(turn)
      window.clearTimeout(land)
      onFlyEnd()
    }
    // The deal fires once, when the card first reaches the hand.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  if (settled) {
    return (
      <motion.div
        layout
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
      initial={{ opacity: 0, x: '-58vw', y: '58vh', rotate: 22, rotateY: 180, scale: 0.8 }}
      animate={{ opacity: 1, x: 0, y: 0, rotate: fan(index, total), rotateY: 0, scale: 1 }}
      exit={{ opacity: 0, y: -60, scale: 0.8 }}
      transition={{
        type: 'spring',
        stiffness: 150,
        damping: 24,
        delay: slot * 0.09,
        opacity: { duration: 0.18, delay: slot * 0.09 },
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
