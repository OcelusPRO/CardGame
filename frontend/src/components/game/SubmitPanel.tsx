import { useEffect, useMemo, useRef, useState } from 'react'
import type { GameView, PunchlineCardView } from '../../api/types'
import { pickAutoAnswer, shouldAutoSubmit } from '../../game/autoAnswer'
import { useCountdown } from '../../hooks/useCountdown'
import { CardHand } from '../cards/CardHand'
import { fillAnswerBlanks } from '../cards/situationParts'
import { SituationCard } from '../cards/SituationCard'
import { WritableCard } from '../cards/WritableCard'
import { Button } from '../ui/Button'
import { RoundStage } from './RoundStage'

interface Props {
  game: GameView
  onPlayCards: (cardIds: string[], fills: string[][]) => void
  onWriteAnswers: (texts: string[]) => void
}

/**
 * Where a player builds their answer, either from the hand or from scratch. The situation
 * card fills in live, so the joke can be read before it is sent, and the send button sits
 * right under it. A card carrying its own holes gets a row of inputs to complete it.
 */
export function SubmitPanel({ game, onPlayCards, onWriteAnswers }: Props) {
  const expected = game.round?.expectedAnswers ?? 1
  const [selected, setSelected] = useState<string[]>([])
  const [texts, setTexts] = useState<string[]>(() => Array(expected).fill(''))
  const [fills, setFills] = useState<Record<string, string[]>>({})
  const writing = game.settings.answerMode === 'FREE_TEXT'
  const remaining = useCountdown(game.deadlineMillis, game.serverTimeMillis)
  const autoSent = useRef<string | null>(null)

  const roundKey = `${game.code}-${game.round?.number ?? 0}`

  const cardById = useMemo(() => {
    const map: Record<string, PunchlineCardView> = {}
    game.you.hand.forEach((card) => {
      map[card.id] = card
    })
    return map
  }, [game.you.hand])

  const fillsFor = (cardId: string): string[] => {
    const card = cardById[cardId]
    const count = card?.blankCount ?? 0
    return Array.from({ length: count }, (_, index) => fills[cardId]?.[index] ?? '')
  }

  const blankCards = selected.map((id) => cardById[id]).filter((card) => card && card.blankCount > 0)
  const blanksReady = blankCards.every((card) =>
    fillsFor(card.id).every((value) => value.trim().length > 0),
  )

  const cardFills = (ids: string[]): string[][] =>
    ids.map((id) => (cardById[id]?.blankCount ? fillsFor(id).map((value) => value.trim()) : []))

  const preview = writing
    ? texts.filter(Boolean)
    : selected.map((id) => {
        const card = cardById[id]
        return card ? fillAnswerBlanks(card.text, fillsFor(id)) : ''
      })

  const ready = writing
    ? texts.filter((text) => text.trim().length > 0).length === expected
    : selected.length === expected && blanksReady

  useEffect(() => {
    setSelected([])
    setTexts(Array(expected).fill(''))
    setFills({})
  }, [roundKey, expected])

  // Nobody sits out a round because they hesitated: what is picked leaves on its own,
  // and an untouched hand plays a random card rather than nothing.
  useEffect(() => {
    if (!game.you.mustAnswer || !shouldAutoSubmit(remaining)) return
    if (autoSent.current === roundKey) return
    autoSent.current = roundKey
    if (writing) {
      const written = texts.map((text) => text.trim())
      if (written.every((text) => text.length > 0)) onWriteAnswers(written)
      return
    }
    const picked = pickAutoAnswer(selected, game.you.hand, expected)
    // A hole left empty at the buzzer is filled with a placeholder so the card is still valid.
    const autoFills = picked.map((id) =>
      cardById[id]?.blankCount
        ? fillsFor(id).map((value) => value.trim() || '…')
        : [],
    )
    onPlayCards(picked, autoFills)
  }, [remaining, roundKey, game.you.mustAnswer, game.you.hand, writing, texts, selected, fills, cardById, expected, onPlayCards, onWriteAnswers])

  if (!game.round) return null

  const send = () =>
    writing ? onWriteAnswers(texts.map((text) => text.trim())) : onPlayCards(selected, cardFills(selected))

  return (
    <RoundStage
      situation={
        <>
          <SituationCard
            card={game.round.situation}
            filledWith={preview}
            footer={`Manche ${game.round.number} · ${expected} réponse${expected > 1 ? 's' : ''}`}
          />
          {game.you.mustAnswer && blankCards.length > 0 && (
            <div className="sketch mt-3 flex flex-col gap-3 bg-paper/70 p-3">
              {blankCards.map((card) => (
                <div key={card.id} className="flex flex-col gap-1.5">
                  <span className="truncate text-xs font-semibold uppercase tracking-wider text-ink/60">
                    {card.text}
                  </span>
                  <div className="flex flex-wrap gap-2">
                    {Array.from({ length: card.blankCount }, (_, index) => (
                      <input
                        key={index}
                        value={fills[card.id]?.[index] ?? ''}
                        maxLength={120}
                        aria-label={`Compléter « ${card.text} » — trou ${index + 1}`}
                        placeholder={`Trou ${index + 1}`}
                        onChange={(event) =>
                          setFills((current) => {
                            const next = [...(current[card.id] ?? Array(card.blankCount).fill(''))]
                            next[index] = event.target.value
                            return { ...current, [card.id]: next }
                          })
                        }
                        className="min-w-32 flex-1 sketch-input bg-paper px-3 py-1.5 text-sm outline-none focus:border-punch"
                      />
                    ))}
                  </div>
                </div>
              ))}
            </div>
          )}
          {game.you.mustAnswer && (
            <div className="fixed inset-x-4 bottom-4 z-40 lg:static">
              <Button full disabled={!ready} onClick={send}>
                {ready ? 'Envoyer ma réponse' : sendHint(selected.length, expected, blankCards.length > 0 && !blanksReady)}
              </Button>
            </div>
          )}
        </>
      }
    >
      {game.you.isCzar && !game.you.mustAnswer ? (
        <p className="py-10 text-center font-display text-lg text-ink/70">
          Vous tranchez cette manche. Laissez les autres se ridiculiser.
        </p>
      ) : game.you.mustAnswer ? (
        writing ? (
          <div className="grid justify-center gap-4 sm:grid-cols-[repeat(auto-fit,minmax(14rem,20rem))]">
            {texts.map((text, index) => (
              <WritableCard
                key={index}
                label={`Réponse ${index + 1}`}
                value={text}
                onChange={(value) => setTexts(texts.map((old, i) => (i === index ? value : old)))}
                onSubmit={() => {
                  if (ready) send()
                }}
              />
            ))}
          </div>
        ) : (
          <CardHand
            cards={game.you.hand}
            selected={selected}
            fills={fills}
            onToggle={(cardId) => setSelected(toggle(selected, cardId, expected))}
          />
        )
      ) : (
        <p className="py-10 text-center font-display text-lg text-mint">
          Réponse envoyée. On attend les retardataires…
        </p>
      )}
    </RoundStage>
  )
}

function sendHint(picked: number, expected: number, needsBlanks: boolean): string {
  if (picked !== expected) return `Choisissez ${expected} réponse${expected > 1 ? 's' : ''}`
  if (needsBlanks) return 'Complétez les trous de la carte'
  return `Choisissez ${expected} réponse${expected > 1 ? 's' : ''}`
}

/** Selecting behaves like a queue: past the limit, the oldest pick drops out. */
function toggle(selected: string[], cardId: string, limit: number): string[] {
  if (selected.includes(cardId)) return selected.filter((id) => id !== cardId)
  const next = [...selected, cardId]
  return next.length > limit ? next.slice(next.length - limit) : next
}
