import { useEffect, useState } from 'react'
import type { AnswerView, GameView } from '../../api/types'
import { playSound } from '../../audio/engine'
import { SituationCard } from '../cards/SituationCard'
import { AnswerCard } from './AnswerCard'
import { RoundStage } from './RoundStage'

interface Props {
  game: GameView
  onChoose: (answerId: number) => void
}

/**
 * The judging step. Pointing at an answer writes it straight into the situation card, so
 * the joke is read the way it will be scored before anybody commits a vote.
 */
export function VotePanel({ game, onChoose }: Props) {
  const [previewId, setPreviewId] = useState<number | null>(null)
  const round = game.round
  const czarMode = game.settings.selectionMode === 'CZAR'
  const canChoose = game.you.mustVote
  // The host can allow voting for one's own answer, but only in the everybody-votes mode.
  const canVoteOwn = !czarMode && game.settings.allowSelfVote

  useEffect(() => setPreviewId(null), [round?.number])

  if (!round) return null

  const shown = answerOf(round.answers, previewId ?? round.myVote ?? null)

  return (
    <RoundStage
      situation={
        <SituationCard
          card={round.situation}
          filledWith={shown?.texts ?? []}
          footer={`Manche ${round.number}`}
        />
      }
    >
      <div className="flex flex-col gap-4">
        <p className="text-center font-display text-lg text-ink/75">
          {canChoose
            ? czarMode
              ? 'À vous de trancher.'
              : 'Votez pour la meilleure réponse.'
            : czarMode
              ? 'Le maître du jeu délibère…'
              : 'Vote enregistré, on attend les autres.'}
        </p>

        <div className="grid gap-4 grid-cols-[repeat(auto-fit,minmax(13rem,1fr))]">
          {round.answers.map((answer) => {
            const blocked = answer.isMine && !canVoteOwn
            return (
              <AnswerCard
                key={answer.id}
                answer={answer}
                voted={round.myVote === answer.id}
                disabled={!canChoose || blocked}
                onPreview={() => setPreviewId(answer.id)}
                onVote={
                  canChoose && !blocked
                    ? () => {
                        playSound('vote')
                        onChoose(answer.id)
                      }
                    : undefined
                }
              />
            )
          })}
        </div>
      </div>
    </RoundStage>
  )
}

function answerOf(answers: AnswerView[], id: number | null): AnswerView | undefined {
  return id === null ? undefined : answers.find((answer) => answer.id === id)
}
