import { useEffect, useState } from 'react'
import type { AnswerView, ChatVotesView, GameView } from '../../api/types'
import { playSound } from '../../audio/engine'
import { SituationCard } from '../cards/SituationCard'
import { AnswerCard } from './AnswerCard'
import { ChatVoteNotice } from './ChatVoteNotice'
import { RoundStage } from './RoundStage'

interface Props {
  game: GameView
  onChoose: (answerId: number) => void
}

/**
 * The judging step. Pointing at an answer writes it straight into the situation card, so
 * the joke is read the way it will be scored before anybody commits a vote.
 *
 * When the chat is the judge, nobody at the table votes: every answer wears the number
 * the viewers type, and the count coming back from the chats is shown live under each one.
 */
export function VotePanel({ game, onChoose }: Props) {
  const [previewId, setPreviewId] = useState<number | null>(null)
  const round = game.round
  const czarMode = game.settings.selectionMode === 'CZAR'
  // The chat judges alone: nobody at the table gets a say this round.
  const chatMode = game.settings.selectionMode === 'CHAT'
  const canChoose = game.you.mustVote
  // The host can allow voting for one's own answer, but only in the everybody-votes mode.
  const canVoteOwn = !czarMode && game.settings.allowSelfVote
  // The server only fills the channels in once a chat is actually being read.
  const chatVoting = game.chatChannels.length > 0

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
          {chatMode
            ? 'Le tchat tranche : regardez les votes tomber.'
            : canChoose
              ? czarMode
                ? 'À vous de trancher.'
                : 'Votez pour la meilleure réponse.'
              : czarMode
                ? 'Le maître du jeu délibère…'
                : 'Vote enregistré, on attend les autres.'}
        </p>

        {chatVoting && (
          <ChatVoteNotice channels={game.chatChannels} viewers={totalChatVotes(round.answers)} />
        )}

        <div className="grid gap-4 grid-cols-[repeat(auto-fit,minmax(13rem,1fr))]">
          {round.answers.map((answer) => {
            const blocked = answer.isMine && !canVoteOwn
            return (
              <AnswerCard
                key={answer.id}
                answer={answer}
                voted={round.myVote === answer.id}
                disabled={!canChoose || blocked}
                number={chatVoting ? answer.id + 1 : undefined}
                chatVotes={chatVoting ? (answer.chatVotes ?? EMPTY_CHAT_VOTES) : undefined}
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

const EMPTY_CHAT_VOTES: ChatVotesView = { count: 0, voters: [] }

function totalChatVotes(answers: AnswerView[]): number {
  return answers.reduce((total, answer) => total + (answer.chatVotes?.count ?? 0), 0)
}
