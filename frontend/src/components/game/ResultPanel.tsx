import { motion } from 'motion/react'
import type { AnswerView, GameView, RoundOutcomeView } from '../../api/types'
import { SituationCard } from '../cards/SituationCard'
import { Button } from '../ui/Button'
import { AnswerCard } from './AnswerCard'
import { CrumpledAnswer } from './CrumpledAnswer'
import { RoundStage } from './RoundStage'

interface Props {
  game: GameView
  onNext: () => void
}

/** The reveal: the winning answer written large into the situation, then the tally. */
export function ResultPanel({ game, onNext }: Props) {
  const round = game.round
  if (!round?.outcome) return null
  const outcome = round.outcome
  const ranked = [...round.answers].sort((a, b) => (b.votes ?? 0) - (a.votes ?? 0))
  const winning = winningAnswer(ranked, outcome)

  return (
    <RoundStage
      situation={
        <SituationCard
          card={round.situation}
          filledWith={winning?.texts ?? []}
          celebrate={Boolean(winning)}
          footer={winnerFooter(game, outcome)}
        />
      }
    >
      <div className="flex flex-col gap-5">
        <motion.ul layout className="flex flex-wrap justify-center gap-3">
          {Object.entries(outcome.points).map(([playerId, points]) => (
            <motion.li
              key={playerId}
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              className="sketch-pill [--stroke:var(--color-mint)] bg-mint/15 px-4 py-2 font-display font-bold text-mint"
            >
              {game.players.find((player) => player.id === playerId)?.nickname ?? '?'} +{points}
            </motion.li>
          ))}
          {Object.keys(outcome.points).length === 0 && (
            <li className="text-sm text-ink/60">Personne n'a marqué. Ça arrive.</li>
          )}
        </motion.ul>

        <motion.div layout className="grid gap-4 grid-cols-[repeat(auto-fit,minmax(13rem,1fr))]">
          {ranked.map((answer, index) => {
            const won = answer.authorId !== undefined && outcome.winners.includes(answer.authorId)
            const card = (
              <AnswerCard
                answer={answer}
                author={game.players.find((player) => player.id === answer.authorId)}
                winner={won}
              />
            )
            // Everything that fell short of the win is balled up and thrown away, last
            // place first, so the table is left with the answer that actually landed.
            return won ? (
              <div key={answer.id}>{card}</div>
            ) : (
              <CrumpledAnswer
                key={answer.id}
                delay={2 + (ranked.length - index) * 0.25}
                face={{
                  text: answer.texts.join(' · '),
                  author: game.players.find((player) => player.id === answer.authorId)?.nickname ?? 'Anonyme',
                  votes: answer.votes,
                }}
              >
                {card}
              </CrumpledAnswer>
            )
          })}
        </motion.div>

        {game.you.isHost && (
          <Button full onClick={onNext}>
            Manche suivante
          </Button>
        )}
      </div>
    </RoundStage>
  )
}

/** The answer to celebrate: the one the server put on top, else the best voted winner. */
function winningAnswer(ranked: AnswerView[], outcome: RoundOutcomeView): AnswerView | undefined {
  const top = ranked.find((answer) => answer.id === outcome.topAnswerId)
  if (top) return top
  return ranked.find((answer) => answer.authorId !== undefined && outcome.winners.includes(answer.authorId))
}

function winnerFooter(game: GameView, outcome: RoundOutcomeView): string {
  if (outcome.winners.length === 0) return 'Aucun gagnant cette manche'
  const names = outcome.winners
    .map((id) => game.players.find((player) => player.id === id)?.nickname ?? '?')
    .join(', ')
  return outcome.winners.length > 1 ? `Ex æquo : ${names}` : `Gagnant : ${names}`
}
