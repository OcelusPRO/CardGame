import { motion } from 'motion/react'
import type { AnswerView, PlayerView } from '../../api/types'
import { GameCard } from '../cards/GameCard'
import { WRAP_CLASSES, punchlineFontSize } from '../cards/textScale'
import { Avatar } from '../avatar/Avatar'

interface Props {
  answer: AnswerView
  author?: PlayerView
  onVote?: () => void
  onPreview?: () => void
  voted?: boolean
  winner?: boolean
  disabled?: boolean
  /** The number the Twitch chat types to pick this answer, shown only when a chat votes. */
  number?: number
  /** Viewers who picked it so far, counted live while the chat votes. */
  chatVotes?: number
}

/** One answer on the table, revealed a little more at every step of the round. */
export function AnswerCard({
  answer,
  author,
  onVote,
  onPreview,
  voted,
  winner,
  disabled,
  number,
  chatVotes,
}: Props) {
  const label = answer.texts.join(' · ')

  return (
    <motion.div
      layout
      initial={{ opacity: 0, rotateY: 90 }}
      animate={{ opacity: 1, rotateY: 0 }}
      transition={{ type: 'spring', stiffness: 200, damping: 20 }}
      className="card-stage h-full"
    >
      <GameCard
        tone="punchline"
        shape="flexible"
        onClick={onVote}
        onHover={onPreview}
        selected={voted}
        disabled={disabled}
        ariaLabel={label}
        footer={
          <span className="flex items-center gap-2">
            {author && <Avatar avatar={author.avatar} size={30} title={author.nickname} />}
            {author?.nickname ?? (answer.isMine ? 'Votre réponse' : 'Anonyme')}
            {answer.votes !== undefined && <span className="ml-auto">{answer.votes} vote(s)</span>}
            {chatVotes !== undefined && answer.votes === undefined && (
              <span className="ml-auto text-[#772ce8]">{chatVotes} tchat</span>
            )}
          </span>
        }
        className={winner ? 'ring-3 ring-zap' : ''}
      >
        {number !== undefined && (
          <span
            className="mb-2 inline-flex size-7 items-center justify-center rounded-full bg-[#9146FF] font-display text-sm font-black text-white"
          >
            {number}
          </span>
        )}
        <p
          lang="fr"
          style={{ fontSize: punchlineFontSize(label) }}
          // `pre-line` keeps the line breaks a player typed on their card and still
          // collapses the ordinary runs of spaces around them.
          className={`whitespace-pre-line font-display leading-snug font-semibold ${WRAP_CLASSES}`}
        >
          {label}
        </p>
        {winner && (
          <motion.span
            initial={{ scale: 0 }}
            animate={{ scale: 1 }}
            className="absolute -right-2 -top-2 rounded-full bg-zap px-3 py-1 font-display text-sm font-black text-ink"
          >
            🏆
          </motion.span>
        )}
      </GameCard>
    </motion.div>
  )
}
