import { useEffect, useState } from 'react'
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

/** The table where official cards are written, corrected and dropped. */
export function CardEditor({ title, hint, cards, packs, onSave, onDelete }: Props) {
  const [text, setText] = useState('')
  const [packId, setPackId] = useState('')
  const [editing, setEditing] = useState<string | undefined>(undefined)

  // Packs arrive after the first render, so the selection has to catch up with them:
  // left alone, the select would show a pack while holding an empty value.
  useEffect(() => {
    setPackId((current) => (packs.some((pack) => pack.id === current) ? current : (packs[0]?.id ?? '')))
  }, [packs])

  const submit = () => {
    if (!text.trim() || !packId) return
    onSave(packId, text.trim(), editing)
    setText('')
    setEditing(undefined)
  }

  // Only ever show the cards of the pack currently in focus, never the other packs'.
  const visible = packId ? cards.filter((card) => card.packId === packId) : cards

  return (
    <div className="flex flex-col gap-4">
      <div>
        <h3 className="font-display text-lg font-bold">{title}</h3>
        <p className="text-xs text-white/50">{hint}</p>
      </div>

      <div className="flex flex-wrap gap-2">
        <select
          value={packId}
          aria-label="Pack de la carte"
          onChange={(event) => setPackId(event.target.value)}
          className="rounded-full bg-white/10 px-4 py-2 text-sm outline-none ring-1 ring-white/15"
        >
          {packs.length === 0 && <option value="">Aucun pack</option>}
          {packs.map((pack) => (
            <option key={pack.id} value={pack.id} className="bg-ink-soft">
              {pack.name}
            </option>
          ))}
        </select>
        <input
          value={text}
          onChange={(event) => setText(event.target.value)}
          onKeyDown={(event) => event.key === 'Enter' && submit()}
          placeholder="Texte de la carte"
          className="min-w-48 flex-1 rounded-full bg-white/10 px-4 py-2 text-sm outline-none ring-1 ring-white/15 focus:ring-2 focus:ring-punch"
        />
        <Button variant="zap" disabled={!text.trim() || !packId} onClick={submit}>
          {editing ? 'Mettre à jour' : 'Ajouter'}
        </Button>
      </div>

      <p className="text-xs text-white/40">
        {visible.length} carte(s){packId ? ' dans ce pack' : ''}
      </p>

      <ul className="max-h-80 divide-y divide-white/10 overflow-y-auto rounded-2xl bg-white/5">
        {visible.map((card) => (
          <li key={card.id} className="flex items-center gap-3 px-4 py-2 text-sm">
            <span className="min-w-0 flex-1 truncate">{card.text}</span>
            {card.blankCount !== undefined && (
              <span className="rounded-full bg-white/10 px-2 py-0.5 text-xs">{card.blankCount} trou(s)</span>
            )}
            <button
              type="button"
              aria-label={`Modifier ${card.text}`}
              onClick={() => {
                setText(card.text)
                setPackId(card.packId)
                setEditing(card.id)
              }}
              className="text-white/40 transition hover:text-zap"
            >
              ✎
            </button>
            <button
              type="button"
              aria-label={`Supprimer ${card.text}`}
              onClick={() => onDelete(card.id)}
              className="text-white/40 transition hover:text-red-300"
            >
              ✕
            </button>
          </li>
        ))}
        {visible.length === 0 && <li className="px-4 py-3 text-sm text-white/50">Aucune carte.</li>}
      </ul>
    </div>
  )
}
