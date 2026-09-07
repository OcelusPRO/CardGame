import { Fragment, useEffect, useState } from 'react'
import type { AnswerView, ChatVotesView, GameView } from '../../api/types'
import { playSound } from '../../audio/engine'
import type { LiveChatVotes } from '../../game/gameStore'
import { canVoteOwnAnswer } from '../../game/selfVote'
import { SituationCard } from '../cards/SituationCard'
import { AnswerCard } from './AnswerCard'
import { ChatVoteNotice } from './ChatVoteNotice'
import { RoundStage } from './RoundStage'

interface Props {
  game: GameView
  onChoose: (answerId: number) => void
  /** The running Twitch tally, when a chat is the one judging the duel. */
  liveChatVotes?: LiveChatVotes
}

/**
 * The judging step when the round runs as a ladder: two answers, one pick, and the winner
 * moves up.
 *
 * Who does the picking is a separate setting, so this screen serves all three — the table,
 * a rotating czar, or the chat. That is the whole appeal of the pairing: a chat of four
 * thousand types `1` or `2` instead of hunting for a card among twelve, and a czar reads
 * two answers at a time instead of a wall of them.
 */
export function DuelPanel({ game, onChoose, liveChatVotes = {} }: Props) {
  const [previewId, setPreviewId] = useState<number | null>(null)
  const round = game.round
  const bracket = round?.bracket
  const duelNumber = bracket?.duelNumber
  const tier = bracket?.tier

  // Every duel starts on a clean slate: the situation shows the pair, not the last pick.
  useEffect(() => setPreviewId(null), [duelNumber, tier])

  if (!round || !bracket) return null

  const czarMode = game.settings.selectionMode === 'CZAR'
  const chatMode = game.settings.selectionMode === 'CHAT'
  // The server only fills the channels in once a chat is actually being read.
  const chatVoting = game.chatChannels.length > 0
  const left = answerOf(round.answers, bracket.left)
  const right = answerOf(round.answers, bracket.right)
  const facing = [left, right].filter((answer): answer is AnswerView => answer !== undefined)
  const canChoose = game.you.mustVote
  const canVoteOwn = canVoteOwnAnswer(game)
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
        <div className="text-center">
          <p className="font-display text-lg text-ink/75">
            {prompt(chatMode, czarMode, canChoose, Boolean(game.spectator))}
          </p>
          <p className="text-xs font-semibold uppercase tracking-wider text-ink/55">
            {tierLabel(bracket.tier, bracket.duelCount)} · duel {bracket.duelNumber} / {bracket.duelCount}
          </p>
        </div>

        {chatVoting && (
          <ChatVoteNotice active={chatVoting} viewers={totalChatVotes(facing, liveChatVotes)} />
        )}

        <div className="grid items-stretch gap-3 sm:grid-cols-[1fr_auto_1fr]">
          {facing.map((answer, index) => {
            const blocked = answer.isMine && !canVoteOwn
            const card = (
              <AnswerCard
                answer={answer}
                voted={round.myVote === answer.id}
                disabled={!canChoose || blocked}
                voteLabel="duel(s)"
                // On a ladder the viewers pick a side, not a card: `1` and `2`, always.
                number={chatVoting ? index + 1 : undefined}
                chatVotes={
                  chatVoting
                    ? (liveChatVotes[answer.id] ?? answer.chatVotes ?? EMPTY_CHAT_VOTES)
                    : undefined
                }
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
            if (index === 0) return <div key={answer.id}>{card}</div>
            return (
              <Fragment key={answer.id}>
                <span
                  aria-hidden
                  className="self-center text-center font-display text-2xl font-black text-ink/30"
                >
                  vs
                </span>
                {card}
              </Fragment>
            )
          })}
        </div>

        {bracket.wins.length > 0 && (
          <p className="text-center text-xs text-ink/55">
            {bracket.wins.reduce((total, entry) => total + entry.wins, 0)} duel(s) déjà remporté(s) —
            chacun vaut {game.settings.pointsPerVote} point(s).
          </p>
        )}
      </div>
    </RoundStage>
  )
}

function prompt(
  chatMode: boolean,
  czarMode: boolean,
  canChoose: boolean,
  spectator: boolean,
): string {
  if (chatMode) return 'Le tchat départage les deux.'
  if (canChoose) return czarMode ? 'À vous de trancher : laquelle des deux ?' : 'Laquelle des deux ?'
  if (czarMode) return 'Le maître du jeu délibère…'
  // The stream page has no duel to have settled; it is watching the table settle it.
  if (spectator) return 'La table départage les deux…'
  return 'Duel tranché, on attend les autres.'
}

/** The last tier of a ladder is the final, whatever number it happens to carry. */
function tierLabel(tier: number, duelCount: number): string {
  if (duelCount === 1) return 'Finale'
  if (duelCount === 2) return 'Demi-finales'
  return `Tour ${tier}`
}

function answerOf(answers: AnswerView[], id: number | null | undefined): AnswerView | undefined {
  return id === null || id === undefined ? undefined : answers.find((answer) => answer.id === id)
}

const EMPTY_CHAT_VOTES: ChatVotesView = { count: 0, voters: [] }

/** Only the two answers on the table count: the tally restarts at every duel. */
function totalChatVotes(facing: AnswerView[], live: LiveChatVotes): number {
  return facing.reduce(
    (total, answer) => total + (live[answer.id]?.count ?? answer.chatVotes?.count ?? 0),
    0,
  )
}
