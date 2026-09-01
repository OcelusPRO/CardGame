import { useEffect, useMemo, useState } from 'react'
import type { CardAdminView, PackAdminView } from '../../api/adminTypes'
import { Button } from '../ui/Button'

interface Props {
  title: string
  hint: string
  cards: CardAdminView[]
  packs: PackAdminView[]
  onSave: (packId: string, text: string, id?: string) => void
  onDelete: (id: string) => void
}

/** The table where official cards are searched, written, corrected and dropped. */
export function CardEditor({ title, hint, cards, packs, onSave, onDelete }: Props) {
  const [text, setText] = useState('')
  const [packId, setPackId] = useState('')
  const [query, setQuery] = useState('')
  const [editing, setEditing] = useState<string | undefined>(undefined)

  // Packs arrive after the first render, so the selection has to catch up with them:
  // left alone, the select would show a pack while holding an empty value.
  useEffect(() => {
    setPackId((current) => (packs.some((pack) => pack.id === current) ? current : (packs[0]?.id ?? '')))
  }, [packs])

  const reset = () => {
    setText('')
    setEditing(undefined)
  }

  const submit = () => {
    if (!text.trim() || !packId) return
    onSave(packId, text.trim(), editing)
    reset()
  }

  const startEdit = (card: CardAdminView) => {
    setText(card.text)
    setPackId(card.packId)
    setEditing(card.id)
  }

  // Only ever the cards of the pack in focus, then narrowed by the search box.
  const inPack = useMemo(
    () => (packId ? cards.filter((card) => card.packId === packId) : cards),
    [cards, packId],
  )
  const visible = useMemo(() => {
    const needle = query.trim().toLowerCase()
    return needle ? inPack.filter((card) => card.text.toLowerCase().includes(needle)) : inPack
  }, [inPack, query])

  return (
    <div className="flex flex-col gap-4">
      <div>
        <h3 className="font-display text-lg font-bold">{title}</h3>
        <p className="text-xs text-ink/60">{hint}</p>
      </div>

      <div className="flex flex-wrap gap-2">
        <select
          value={packId}
          aria-label="Pack de la carte"
          onChange={(event) => setPackId(event.target.value)}
          className="sketch-input bg-paper px-4 py-2 text-sm outline-none"
        >
          {packs.length === 0 && <option value="">Aucun pack</option>}
          {packs.map((pack) => (
            <option key={pack.id} value={pack.id} className="bg-paper">
              {pack.name}
            </option>
          ))}
        </select>
        <input
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="Rechercher une carte…"
          aria-label={`Rechercher dans ${title.toLowerCase()}`}
          className="min-w-40 flex-1 sketch-input bg-paper px-4 py-2 text-sm outline-none focus:border-punch"
        />
      </div>

      <div className="flex flex-wrap gap-2">
        <input
          value={text}
          onChange={(event) => setText(event.target.value)}
          onKeyDown={(event) => event.key === 'Enter' && submit()}
          placeholder={editing ? 'Nouveau texte de la carte' : 'Texte de la carte'}
          aria-label={editing ? 'Texte de la carte à modifier' : 'Texte de la nouvelle carte'}
          className="min-w-48 flex-1 sketch-input bg-paper px-4 py-2 text-sm outline-none focus:border-punch"
        />
        <Button variant="zap" disabled={!text.trim() || !packId} onClick={submit}>
          {editing ? 'Mettre à jour' : 'Ajouter'}
        </Button>
        {editing && (
          <Button variant="ghost" onClick={reset}>
            Annuler
          </Button>
        )}
      </div>

      <p className="text-xs text-ink/50">
        {query.trim() ? `${visible.length} sur ${inPack.length}` : `${visible.length}`} carte(s)
        {packId ? ' dans ce pack' : ''}
      </p>

      <ul className="max-h-80 divide-y divide-ink/15 overflow-y-auto sketch bg-paper/70">
        {visible.map((card) => (
          <li
            key={card.id}
            className={`flex items-center gap-3 px-4 py-2 text-sm ${
              editing === card.id ? 'bg-punch/15 ring-1 ring-inset ring-punch' : ''
            }`}
          >
            <span className="min-w-0 flex-1 truncate">{card.text}</span>
            {card.blankCount !== undefined && (
              <span className="sketch-pill bg-paper px-2 py-0.5 text-xs">{card.blankCount} trou(s)</span>
            )}
            <button
              type="button"
              aria-label={`Modifier ${card.text}`}
              onClick={() => startEdit(card)}
              className="text-ink/50 transition hover:text-honey"
            >
              ✎
            </button>
            <button
              type="button"
              aria-label={`Supprimer ${card.text}`}
              onClick={() => onDelete(card.id)}
              className="text-ink/50 transition hover:text-red-300"
            >
              ✕
            </button>
          </li>
        ))}
        {visible.length === 0 && (
          <li className="px-4 py-3 text-sm text-ink/60">
            {inPack.length === 0 ? 'Aucune carte.' : 'Aucune carte ne correspond à la recherche.'}
          </li>
        )}
      </ul>
    </div>
  )
}
