import { useCallback, useMemo, useState } from 'react'
import type { ChatCardLogView, ChatWrittenCardView } from '../../api/types'
import { useLocalStorage } from '../../hooks/useLocalStorage'

const STORAGE_KEY = 'cardgame.chatCards.refused'

/** The refusals of one table. A new code starts a new sheet: they never apply to another game. */
interface Refusals {
  code: string
  ids: string[]
}

export interface ChatCardInbox {
  /** The cards not yet written into the host's boxes, per pile, oldest first. */
  fresh: { situations: ChatWrittenCardView[]; punchlines: ChatWrittenCardView[] }
  /** Records that these cards are now lines in the boxes, so they are not written twice. */
  keep: (cards: ChatWrittenCardView[]) => void
  /** The host deleted these lines: the cards behind them are never offered again. */
  refuse: (texts: string[]) => void
}

/**
 * What the chat wrote, on its way into the host's own card boxes.
 *
 * The log the server sends only grows, which is what makes a refusal something to
 * remember: a card the host deleted would otherwise be written back into the box on the
 * very next snapshot. Kept cards are forgotten when the page is, and rightly so — the
 * boxes are empty again too, so everything the chat wrote is offered afresh. A refusal
 * outlives the reload, because a deleted line coming back would be a bug in the host's eyes.
 */
export function useChatCardInbox(code: string, log: ChatCardLogView | undefined): ChatCardInbox {
  const [stored, setStored] = useLocalStorage<Refusals>(STORAGE_KEY, { code: '', ids: [] })
  const [kept, setKept] = useState<string[]>([])

  // Refusals from another table are none of this one's business, and are dropped rather
  // than piling up: one game's sheet at a time is all this ever needs to hold.
  const refused = useMemo(
    () => (stored.code === code ? stored.ids : []),
    [stored, code],
  )

  const known = useMemo(() => new Set([...kept, ...refused]), [kept, refused])

  const fresh = useMemo(
    () => ({
      situations: (log?.situations ?? []).filter((card) => !known.has(card.id)),
      punchlines: (log?.punchlines ?? []).filter((card) => !known.has(card.id)),
    }),
    [log, known],
  )

  const keep = useCallback((cards: ChatWrittenCardView[]) => {
    if (cards.length === 0) return
    setKept((before) => [...before, ...cards.map((card) => card.id)])
  }, [])

  const refuse = useCallback(
    (texts: string[]) => {
      if (texts.length === 0 || !log) return
      const deleted = new Set(texts)
      const ids = [...log.situations, ...log.punchlines]
        .filter((card) => deleted.has(card.text.trim()))
        .map((card) => card.id)
      if (ids.length === 0) return
      setStored({ code, ids: [...new Set([...refused, ...ids])] })
    },
    [log, code, refused, setStored],
  )

  return { fresh, keep, refuse }
}
