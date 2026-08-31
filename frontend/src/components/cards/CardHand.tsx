import { AnimatePresence, motion } from 'motion/react'
import type { PunchlineCardView } from '../../api/types'
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
 */
export function CardHand({ cards, selected, onToggle, disabled = false, fills = {} }: Props) {
  return (
    <div className="-mx-4 overflow-x-auto px-4 pb-6 pt-8 sm:mx-0 sm:overflow-visible sm:px-0">
      <div className="flex min-w-max gap-3 sm:grid sm:min-w-0 sm:grid-cols-[repeat(auto-fit,minmax(11rem,1fr))] sm:gap-4">
        <AnimatePresence initial={false}>
          {cards.map((card, index) => {
            const order = selected.indexOf(card.id)
            return (
              <motion.div
                key={card.id}
                layout
                initial={{ opacity: 0, y: 40, rotate: 0 }}
                animate={{ opacity: 1, y: 0, rotate: fan(index, cards.length) }}
                exit={{ opacity: 0, y: -60, scale: 0.8 }}
                transition={{ type: 'spring', stiffness: 260, damping: 24 }}
                className="w-44 shrink-0 sm:w-auto"
              >
                <PunchlineCard
                  card={card}
                  selected={order >= 0}
                  order={order >= 0 ? order + 1 : undefined}
                  disabled={disabled}
                  fills={fills[card.id]}
                  onClick={disabled ? undefined : () => onToggle(card.id)}
                />
              </motion.div>
            )
          })}
        </AnimatePresence>
      </div>
    </div>
  )
}

/** A gentle arc on the scrolling row: the outer cards lean away, the middle stays upright. */
function fan(index: number, total: number): number {
  if (total < 2) return 0
  const middle = (total - 1) / 2
  return ((index - middle) / middle) * 2
}
