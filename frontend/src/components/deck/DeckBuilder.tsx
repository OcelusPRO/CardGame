import { useEffect, useRef, useState } from 'react'
import type { CardPackView, ChatCardLogView, DeckInput } from '../../api/types'
import { Button } from '../ui/Button'
import { DeckOption } from './DeckOption'
import { linesToCards, removedLines, withLines, withoutLines } from './linesToCards'
import type { SavedDeck } from './SavedDeck'
import { useChatCardInbox } from './useChatCardInbox'
import { useSavedDecks } from './useSavedDecks'

const LAST_PACKS_KEY = 'cardgame.lastSelectedPackIds'
const LAST_DECKS_KEY = 'cardgame.lastSelectedDeckIds'

/** A private window, or blocked site data, makes localStorage throw — read/write guarded. */
function readIds(key: string): string[] {
  try {
    const raw = window.localStorage.getItem(key)
    return raw ? (JSON.parse(raw) as string[]) : []
  } catch {
    return []
  }
}
function writeIds(key: string, ids: string[]) {
  try {
    window.localStorage.setItem(key, JSON.stringify(ids))
  } catch {
    // Storage unavailable: the app keeps working, it just forgets.
  }
}

interface Props {
  packs: CardPackView[]
  disabled: boolean
  /** Which table this is, so a refused chat card stays refused on this one alone. */
  gameCode?: string
  /** What the chats have written, which lands in the boxes below as ordinary lines. */
  chatCards?: ChatCardLogView
  onApply: (deck: DeckInput) => void
}

/** The four things a paquet is made of, in the state they are being edited in. */
interface Composition {
  packIds: string[]
  deckIds: string[]
  situations: string
  punchlines: string
}

/**
 * Where the host composes the paquet: one selector holding the official packs and the
 * decks kept in this browser, plus a place to type cards on the spot. There is no
 * "apply" button — the composed deck is pushed live: toggles at once, the free-text
 * boxes after a short pause so a socket message is not sent on every keystroke.
 *
 * What a Twitch chat writes arrives in those same boxes, as plain lines: the host reads
 * them next to their own, refuses one by deleting its line, and saves the rest as a deck
 * like any other. That is the whole point of putting them there rather than in a list of
 * their own — a stream's good ideas end up in a deck the host reloads next time and grows
 * again, and every custom deck is opened and edited the same way, whoever wrote it.
 *
 * Nothing is ticked on a brand new table: the host picks the packs and decks for this
 * game. What gets picked is remembered in this browser, so chaining another game right
 * after does not mean re-ticking everything from scratch.
 */
export function DeckBuilder({ packs, disabled, gameCode, chatCards, onApply }: Props) {
  const { decks, save, update, remove } = useSavedDecks()
  const inbox = useChatCardInbox(gameCode ?? '', chatCards)
  const [selectedPacks, setSelectedPacks] = useState<string[]>([])
  const [selectedDecks, setSelectedDecks] = useState<string[]>([])
  const [situations, setSituations] = useState('')
  const [punchlines, setPunchlines] = useState('')
  const [deckName, setDeckName] = useState('')
  const [editingId, setEditingId] = useState<string | null>(null)
  const [removingId, setRemovingId] = useState<string | null>(null)

  const build = (it: Composition, from: SavedDeck[] = decks): DeckInput => {
    const chosen = from.filter((deck) => it.deckIds.includes(deck.id))
    return {
      packIds: it.packIds,
      customSituations: [
        ...chosen.flatMap((deck) => deck.situations),
        ...linesToCards(it.situations),
      ],
      customPunchlines: [
        ...chosen.flatMap((deck) => deck.punchlines),
        ...linesToCards(it.punchlines),
      ],
    }
  }

  const current = (): Composition => ({
    packIds: selectedPacks,
    deckIds: selectedDecks,
    situations,
    punchlines,
  })

  const latest = useRef<Composition>(current())
  latest.current = current()
  const debounce = useRef<ReturnType<typeof setTimeout> | undefined>(undefined)
  useEffect(() => () => clearTimeout(debounce.current), [])

  const push = (deck: DeckInput) => {
    if (!disabled) onApply(deck)
  }
  const pushSoon = () => {
    clearTimeout(debounce.current)
    debounce.current = setTimeout(() => push(build(latest.current)), 500)
  }

  /**
   * A guest's `packs` prop already *is* the host's live pool (see `LobbyPanel`), so a
   * guest simply mirrors it, read-only. The host has their own choice to make: nothing is
   * ticked on a brand new table — a fresh server-side game starts on every pack, but the
   * lobby overrides that the moment it opens — except what this browser remembered from
   * the host's last table, restored once and pushed so what is actually dealt matches
   * what the paquet editor shows. After that, a `packs` change (the answer mode switching)
   * only drops an id that stopped existing; it never re-reads storage over a live choice.
   */
  const restoredSelection = useRef(false)
  useEffect(() => {
    if (disabled) {
      setSelectedPacks(packs.map((pack) => pack.id))
      return
    }
    if (packs.length === 0) return
    const validPackIds = new Set(packs.map((pack) => pack.id))
    if (!restoredSelection.current) {
      restoredSelection.current = true
      const validDeckIds = new Set(decks.map((deck) => deck.id))
      const restoredPacks = readIds(LAST_PACKS_KEY).filter((id) => validPackIds.has(id))
      const restoredDecks = readIds(LAST_DECKS_KEY).filter((id) => validDeckIds.has(id))
      setSelectedPacks(restoredPacks)
      setSelectedDecks(restoredDecks)
      push(build({ packIds: restoredPacks, deckIds: restoredDecks, situations, punchlines }))
      return
    }
    setSelectedPacks((current) => current.filter((id) => validPackIds.has(id)))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [packs, disabled])

  // Remembered for the next table — never for a guest, whose selection above is only ever
  // a mirror of somebody else's table, not a choice of their own to keep.
  useEffect(() => {
    if (disabled || !restoredSelection.current) return
    writeIds(LAST_PACKS_KEY, selectedPacks)
  }, [disabled, selectedPacks])
  useEffect(() => {
    if (disabled || !restoredSelection.current) return
    writeIds(LAST_DECKS_KEY, selectedDecks)
  }, [disabled, selectedDecks])

  /**
   * The cards the chat wrote, written into the boxes as they come.
   *
   * Nothing is pushed here on purpose: the server has already slipped those very cards
   * into the piles this game draws from, so pushing would only re-shuffle the paquet, once
   * per batch, for as long as the chat keeps writing. The lines travel with the next edit
   * the host makes — and a line they delete leaves the paquet at that same moment.
   */
  useEffect(() => {
    const fresh = inbox.fresh
    if (fresh.situations.length === 0 && fresh.punchlines.length === 0) return
    setSituations((before) => withLines(before, fresh.situations.map((card) => card.text)))
    setPunchlines((before) => withLines(before, fresh.punchlines.map((card) => card.text)))
    inbox.keep([...fresh.situations, ...fresh.punchlines])
  }, [inbox.fresh, inbox.keep])

  const editBox = (setBox: (value: string) => void, before: string, next: string) => {
    // A deleted line is the host refusing a card, whoever wrote it. The ones that came
    // from the chat have to be remembered, or the next snapshot writes them straight back.
    inbox.refuse(removedLines(before, next))
    setBox(next)
    pushSoon()
  }

  const togglePack = (id: string) => {
    if (disabled) return
    const next = toggle(selectedPacks, id)
    setSelectedPacks(next)
    push(build({ ...current(), packIds: next }))
  }
  const toggleDeck = (id: string) => {
    if (disabled) return
    const next = toggle(selectedDecks, id)
    setSelectedDecks(next)
    push(build({ ...current(), deckIds: next }))
  }

  /**
   * Opens a saved deck for editing: its cards move into the boxes, and it is unticked,
   * because the boxes are where they now are. The paquet itself does not change — the same
   * cards, said as lines rather than as a deck — which is what lets a deck be reloaded and
   * grown stream after stream without ever dealing anything twice.
   */
  const editDeck = (id: string) => {
    const deck = decks.find((it) => it.id === id)
    if (!deck) return
    const next: Composition = {
      packIds: selectedPacks,
      deckIds: selectedDecks.filter((it) => it !== deck.id),
      situations: withLines(situations, deck.situations),
      punchlines: withLines(punchlines, deck.punchlines),
    }
    setEditingId(deck.id)
    setDeckName(deck.name)
    setRemovingId(null)
    setSelectedDecks(next.deckIds)
    setSituations(next.situations)
    setPunchlines(next.punchlines)
    push(build(next))
  }

  /** Puts back what [editDeck] took out: the deck ticked again, its lines out of the boxes. */
  const cancelEdit = () => {
    const deck = decks.find((it) => it.id === editingId)
    const next: Composition = {
      packIds: selectedPacks,
      deckIds: deck ? [...selectedDecks, deck.id] : selectedDecks,
      situations: deck ? withoutLines(situations, deck.situations) : '',
      punchlines: deck ? withoutLines(punchlines, deck.punchlines) : '',
    }
    setEditingId(null)
    setDeckName('')
    setSelectedDecks(next.deckIds)
    setSituations(next.situations)
    setPunchlines(next.punchlines)
    push(build(next))
  }

  /**
   * Saves the boxes as a deck — a new one, or the one being edited — then empties them and
   * ticks it. Nothing has moved as far as the table is concerned: the cards were lines,
   * they are a deck, and the same paquet is dealt either way.
   */
  const persist = () => {
    const name = deckName.trim()
    if (!name) return
    const situationCards = linesToCards(situations)
    const punchlineCards = linesToCards(punchlines)
    const deck = editingId
      ? update(editingId, name, situationCards, punchlineCards)
      : save(name, situationCards, punchlineCards)
    // Saving under the name of another deck replaces it, so its tick goes with it.
    const replaced = decks.filter((it) => it.name === name && it.id !== deck.id).map((it) => it.id)
    const deckIds = [
      ...selectedDecks.filter((it) => it !== deck.id && !replaced.includes(it)),
      deck.id,
    ]
    setEditingId(null)
    setDeckName('')
    setSelectedDecks(deckIds)
    setSituations('')
    setPunchlines('')
    // `decks` still holds the deck as it was a moment ago, so it is left out of the merge
    // and the cards just saved into it are added by hand.
    const rest = build({
      packIds: selectedPacks,
      deckIds: deckIds.filter((it) => it !== deck.id),
      situations: '',
      punchlines: '',
    })
    push({
      packIds: rest.packIds,
      customSituations: [...rest.customSituations, ...situationCards],
      customPunchlines: [...rest.customPunchlines, ...punchlineCards],
    })
  }

  const removeDeck = (id: string) => {
    const deck = decks.find((it) => it.id === id)
    setRemovingId(null)
    const editing = editingId === id
    const next: Composition = {
      packIds: selectedPacks,
      deckIds: selectedDecks.filter((it) => it !== id),
      situations: editing && deck ? withoutLines(situations, deck.situations) : situations,
      punchlines: editing && deck ? withoutLines(punchlines, deck.punchlines) : punchlines,
    }
    if (editing) {
      setEditingId(null)
      setDeckName('')
    }
    setSelectedDecks(next.deckIds)
    setSituations(next.situations)
    setPunchlines(next.punchlines)
    remove(id)
    push(build(next, decks.filter((it) => it.id !== id)))
  }

  const written = (chatCards?.situations.length ?? 0) + (chatCards?.punchlines.length ?? 0)
  // A deck named after a game that is gone is nothing to confirm the deletion of.
  const removing = decks.find((deck) => deck.id === removingId)

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap gap-2">
        {packs.map((pack) => (
          <DeckOption
            key={pack.id}
            name={pack.name}
            detail={`${pack.situationCount} situations · ${pack.punchlineCount} réponses`}
            official
            adult={pack.adultOnly}
            selected={selectedPacks.includes(pack.id)}
            disabled={disabled}
            onToggle={() => togglePack(pack.id)}
          />
        ))}
        {decks.map((deck) => (
          <DeckOption
            key={deck.id}
            name={deck.name}
            detail={
              editingId === deck.id
                ? '✏️ ouvert dans les cases ci-dessous'
                : `${deck.situations.length} situations · ${deck.punchlines.length} réponses`
            }
            official={false}
            selected={selectedDecks.includes(deck.id)}
            disabled={disabled}
            onToggle={() => toggleDeck(deck.id)}
          />
        ))}
        {packs.length === 0 && decks.length === 0 && (
          <p className="text-sm text-ink/60">Aucun deck disponible : écrivez les vôtres ci-dessous.</p>
        )}
      </div>

      {!disabled && (
        <p className="text-xs text-ink/50">Les changements de paquet sont appliqués automatiquement.</p>
      )}

      {selectedPacks.length === 0 && selectedDecks.length === 0 && (
        <p className="text-xs text-honey">
          Aucun deck sélectionné : la partie se jouera uniquement sur les cartes écrites ici.
        </p>
      )}

      {written > 0 && (
        <p className="text-xs text-ink/65">
          {written === 1
            ? '1 carte proposée par le tchat a rejoint vos cases ci-dessous.'
            : `${written} cartes proposées par le tchat ont rejoint vos cases ci-dessous.`}{' '}
          Supprimez la ligne pour refuser une carte, ou enregistrez le tout comme un deck.
        </p>
      )}

      <CardTextArea
        label="Vos situations (une par ligne, utilisez ____ pour les trous)"
        value={situations}
        onChange={(value) => editBox(setSituations, situations, value)}
        disabled={disabled}
        placeholder={'Chez moi, on ne parle jamais de ____.\nLe secret de ma réussite : ____.'}
      />
      <CardTextArea
        label="Vos réponses (une par ligne)"
        value={punchlines}
        onChange={(value) => editBox(setPunchlines, punchlines, value)}
        disabled={disabled}
        placeholder={'un poulet rôti mal cuit\nla honte de ma vie'}
      />

      <div className="flex flex-wrap items-center gap-2">
        <input
          value={deckName}
          onChange={(event) => setDeckName(event.target.value)}
          placeholder="Nom du deck"
          className="sketch-input bg-paper px-4 py-2 text-sm outline-none focus:border-punch"
        />
        <Button variant="ghost" disabled={!deckName.trim()} onClick={persist}>
          {editingId ? '💾 Mettre à jour' : '💾 Enregistrer'}
        </Button>
        {editingId && (
          <Button variant="ghost" onClick={cancelEdit}>
            ✕ Annuler
          </Button>
        )}
        {decks.length > 0 && (
          <select
            aria-label="Modifier un deck enregistré"
            value=""
            onChange={(event) => event.target.value && editDeck(event.target.value)}
            className="sketch-input bg-paper px-3 py-2 text-sm outline-none"
          >
            <option value="">✏️ Modifier un deck…</option>
            {decks.map((deck) => (
              <option key={deck.id} value={deck.id} className="bg-paper">
                {deck.name}
              </option>
            ))}
          </select>
        )}
        {decks.length > 0 && (
          <select
            aria-label="Supprimer un deck enregistré"
            value=""
            onChange={(event) => event.target.value && setRemovingId(event.target.value)}
            className="sketch-input bg-paper px-3 py-2 text-sm outline-none"
          >
            <option value="">Supprimer un deck…</option>
            {decks.map((deck) => (
              <option key={deck.id} value={deck.id} className="bg-paper">
                {deck.name}
              </option>
            ))}
          </select>
        )}
      </div>

      {/* A deck lives in this browser and nowhere else, so a mis-click is not something the
          host can undo anywhere: the deletion is asked for once, and named. */}
      {removing && (
        <div role="alert" className="sketch flex flex-wrap items-center gap-3 bg-punch/10 px-4 py-3">
          <p className="text-sm leading-relaxed">
            Supprimer <strong>« {removing.name} »</strong> et ses {removing.situations.length}{' '}
            situations · {removing.punchlines.length} réponses ? Ce deck n'est gardé que dans ce
            navigateur : il n'en existe aucune copie ailleurs.
          </p>
          <div className="flex flex-wrap gap-2">
            <Button variant="danger" onClick={() => removeDeck(removing.id)}>
              🗑 Oui, supprimer
            </Button>
            <Button variant="ghost" onClick={() => setRemovingId(null)}>
              Non, garder
            </Button>
          </div>
        </div>
      )}
    </div>
  )
}

function toggle(values: string[], id: string): string[] {
  return values.includes(id) ? values.filter((value) => value !== id) : [...values, id]
}

interface CardTextAreaProps {
  label: string
  value: string
  onChange: (value: string) => void
  disabled: boolean
  placeholder: string
}

function CardTextArea({ label, value, onChange, disabled, placeholder }: CardTextAreaProps) {
  return (
    <label className="flex flex-col gap-1.5">
      <span className="text-xs font-semibold uppercase tracking-wider text-ink/60">{label}</span>
      <textarea
        value={value}
        disabled={disabled}
        placeholder={placeholder}
        rows={4}
        onChange={(event) => onChange(event.target.value)}
        className="sketch-input bg-paper px-4 py-3 font-hand text-2xl leading-tight outline-none transition placeholder:text-ink/35 focus:border-punch disabled:opacity-40"
      />
    </label>
  )
}
