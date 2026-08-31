import { motion } from 'motion/react'
import type { SituationCardView } from '../../api/types'
import { GameCard } from './GameCard'
import { splitSituation } from './situationParts'
import { WRAP_CLASSES, celebratedAnswerFontSize, punchlineFontSize, situationFontSize } from './textScale'

interface Props {
  card: SituationCardView
  filledWith?: string[]
  footer?: string
  /** Set once the round is decided: the answer is written in, loud and proud. */
  celebrate?: boolean
}

/**
 * The black card. Answers appear written straight into the holes — as a preview while a
 * player builds their joke, and as the verdict once the round is decided.
 */
export function SituationCard({ card, filledWith = [], footer, celebrate = false }: Props) {
  const parts = splitSituation(card.text)
  const preview = card.text + filledWith.join(' ')

  return (
    <GameCard
      tone="situation"
      shape="flexible"
      className="min-h-64 justify-start sm:min-h-72"
      footer={footer ?? (card.custom ? 'Carte maison' : 'Situation')}
    >
      <p
        lang="fr"
        style={{ fontSize: situationFontSize(preview) }}
        className={`font-display leading-snug font-semibold ${WRAP_CLASSES}`}
      >
        {parts.map((part, index) =>
          part.kind === 'text' ? (
            <span key={index}>{part.value}</span>
          ) : (
            <Blank key={index} answer={filledWith[part.blankIndex]} celebrate={celebrate} />
          ),
        )}
      </p>
    </GameCard>
  )
}

function Blank({ answer, celebrate }: { answer?: string; celebrate: boolean }) {
  if (!answer) {
    return <span className="mx-1 inline-block w-24 border-b-3 border-punch align-baseline" />
  }
  return (
    <motion.span
      key={answer}
      initial={{ opacity: 0, y: 10, rotate: -3, scale: celebrate ? 0.7 : 0.95 }}
      animate={{ opacity: 1, y: 0, rotate: -1, scale: 1 }}
      transition={{ type: 'spring', stiffness: 280, damping: 16 }}
      lang="fr"
      style={{ fontSize: celebrate ? celebratedAnswerFontSize(answer) : punchlineFontSize(answer) }}
      className={`mx-1 inline-block font-hand leading-none ${WRAP_CLASSES} ${
        celebrate ? 'text-zap drop-shadow-[0_0_18px_rgba(255,210,63,0.55)]' : 'text-zap'
      }`}
    >
      {answer}
    </motion.span>
  )
}
