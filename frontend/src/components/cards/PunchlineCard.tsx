import type { PunchlineCardView } from '../../api/types'
import { GameCard } from './GameCard'
import { splitAnswerBlanks } from './situationParts'
import { WRAP_CLASSES, punchlineFontSize } from './textScale'

interface Props {
  card: PunchlineCardView
  onClick?: () => void
  selected?: boolean
  disabled?: boolean
  order?: number
  /** Words typed into the card's own holes, shown inline once the player fills them. */
  fills?: string[]
}

/** The white card, the one that carries the joke. */
export function PunchlineCard({ card, onClick, selected, disabled, order, fills = [] }: Props) {
  const hasBlanks = card.blankCount > 0
  const parts = hasBlanks ? splitAnswerBlanks(card.text) : null

  return (
    <GameCard
      tone="punchline"
      onClick={onClick}
      selected={selected}
      disabled={disabled}
      ariaLabel={card.text}
      footer={hasBlanks ? 'À compléter' : card.custom ? 'Carte maison' : 'Réponse'}
    >
      {order !== undefined && (
        <span className="mb-2 inline-flex size-7 items-center justify-center rounded-full bg-punch text-sm font-bold text-white">
          {order}
        </span>
      )}
      <p
        lang="fr"
        style={{ fontSize: punchlineFontSize(card.text) }}
        className={`font-display leading-snug font-semibold ${WRAP_CLASSES}`}
      >
        {parts
          ? parts.map((part, index) =>
              part.kind === 'text' ? (
                <span key={index}>{part.value}</span>
              ) : (
                <Hole key={index} fill={fills[part.blankIndex]} />
              ),
            )
          : card.text}
      </p>
    </GameCard>
  )
}

function Hole({ fill }: { fill?: string }) {
  if (!fill?.trim()) {
    return <span className="mx-0.5 inline-block w-16 border-b-2 border-punch align-baseline" />
  }
  return <span className="mx-0.5 font-hand text-punch">{fill.trim()}</span>
}
