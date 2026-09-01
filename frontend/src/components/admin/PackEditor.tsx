import { useState } from 'react'
import type { PackAdminView } from '../../api/adminTypes'
import { ApiError } from '../../api/ApiError'
import { errorMessage } from '../../lib/errorMessages'
import { Button } from '../ui/Button'

interface PackDraft {
  id?: string
  name: string
  description: string
  answerModeCards: boolean
  answerModeFreeText: boolean
  adultOnly: boolean
  enabled?: boolean
}

interface Props {
  packs: PackAdminView[]
  onSave: (draft: PackDraft) => void
  onDelete: (id: string) => void | Promise<void>
}

/** Creating, editing and dropping the themed packs the cards belong to. */
export function PackEditor({ packs, onSave, onDelete }: Props) {
  const [editing, setEditing] = useState<string | undefined>(undefined)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [cards, setCards] = useState(true)
  const [freeText, setFreeText] = useState(true)
  const [adult, setAdult] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const remove = async (pack: PackAdminView) => {
    setError(null)
    if (!window.confirm(`Supprimer le pack « ${pack.name} » ?`)) return
    try {
      await onDelete(pack.id)
    } catch (failure) {
      setError(
        failure instanceof ApiError
          ? errorMessage(failure.code)
          : 'La suppression a échoué.',
      )
    }
  }

  const reset = () => {
    setEditing(undefined)
    setName('')
    setDescription('')
    setCards(true)
    setFreeText(true)
    setAdult(false)
  }

  const edit = (pack: PackAdminView) => {
    setEditing(pack.id)
    setName(pack.name)
    setDescription(pack.description)
    setCards(pack.answerModeCards)
    setFreeText(pack.answerModeFreeText)
    setAdult(pack.adultOnly)
  }

  const submit = () => {
    onSave({
      id: editing,
      name: name.trim(),
      description: description.trim(),
      answerModeCards: cards,
      answerModeFreeText: freeText,
      adultOnly: adult,
      enabled: editing ? packs.find((pack) => pack.id === editing)?.enabled : true,
    })
    reset()
  }

  return (
    <div className="flex flex-col gap-4">
      <ul className="flex flex-col gap-2">
        {packs.map((pack) => (
          <li
            key={pack.id}
            className={`flex items-center gap-3 sketch-alt px-3 py-2 text-sm ${
              editing === pack.id ? 'bg-ink/5 ring-1 ring-punch' : 'bg-ink/5'
            }`}
          >
            <span className="min-w-0 flex-1">
              <span className="block font-semibold">
                {pack.name}
                {!pack.enabled && <span className="ml-2 text-xs text-ink/50">désactivé</span>}
                {pack.adultOnly && (
                  <span className="ml-2 sketch-pill bg-red-300/20 px-2 py-0.5 text-[11px] font-semibold text-red-300">
                    18+
                  </span>
                )}
              </span>
              <span className="block text-xs text-ink/60">
                {pack.situationCount} situations · {pack.punchlineCount} réponses
              </span>
              <span className="mt-1 flex flex-wrap gap-1.5">
                <ModeChip label="Cartes distribuées" on={pack.answerModeCards} />
                <ModeChip label="Sans limites" on={pack.answerModeFreeText} />
              </span>
            </span>
            <button
              type="button"
              aria-label={`Modifier ${pack.name}`}
              onClick={() => edit(pack)}
              className="text-ink/50 transition hover:text-honey"
            >
              ✎
            </button>
            <button
              type="button"
              aria-label={`Supprimer ${pack.name}`}
              onClick={() => remove(pack)}
              className="text-ink/50 transition hover:text-red-300"
            >
              ✕
            </button>
          </li>
        ))}
        {packs.length === 0 && <li className="text-sm text-ink/60">Aucun pack pour l&apos;instant.</li>}
      </ul>

      {error && <p className="text-sm text-red-300">{error}</p>}

      <div className="sketch flex flex-col gap-3 bg-paper/70 p-3">
        <p className="text-xs font-semibold uppercase tracking-wider text-ink/60">
          {editing ? 'Modifier le pack' : 'Nouveau pack'}
        </p>

        <div className="flex flex-wrap gap-2">
          <input
            value={name}
            onChange={(event) => setName(event.target.value)}
            placeholder="Nom du pack"
            className="flex-1 sketch-input bg-paper px-4 py-2 text-sm outline-none focus:border-punch"
          />
          <input
            value={description}
            onChange={(event) => setDescription(event.target.value)}
            placeholder="Description"
            className="flex-1 sketch-input bg-paper px-4 py-2 text-sm outline-none focus:border-punch"
          />
        </div>

        <fieldset className="flex flex-wrap items-center gap-4 text-sm">
          <legend className="mb-1 text-xs font-semibold uppercase tracking-wider text-ink/60">
            Modes de jeu autorisés
          </legend>
          <label className="flex items-center gap-2">
            <input
              type="checkbox"
              checked={cards}
              onChange={(event) => setCards(event.target.checked)}
              className="size-4 accent-punch"
            />
            Cartes distribuées
          </label>
          <label className="flex items-center gap-2">
            <input
              type="checkbox"
              checked={freeText}
              onChange={(event) => setFreeText(event.target.checked)}
              className="size-4 accent-punch"
            />
            Sans limites (on écrit)
          </label>
        </fieldset>

        <label className="flex items-center gap-2 text-sm">
          <input
            type="checkbox"
            checked={adult}
            onChange={(event) => setAdult(event.target.checked)}
            className="size-4 accent-red-300"
          />
          Interdit aux mineurs (18+) — visible seulement pour les hôtes autorisés
        </label>

        <div className="flex flex-wrap items-center gap-2">
          <Button variant="zap" disabled={!name.trim() || (!cards && !freeText)} onClick={submit}>
            {editing ? 'Mettre à jour' : 'Ajouter'}
          </Button>
          {editing && (
            <Button variant="ghost" onClick={reset}>
              Annuler
            </Button>
          )}
          {!cards && !freeText && (
            <p className="text-xs text-honey">Choisissez au moins un mode de jeu.</p>
          )}
        </div>
      </div>
    </div>
  )
}

/** A pill that reads at a glance whether the pack may be played in that mode. */
function ModeChip({ label, on }: { label: string; on: boolean }) {
  return (
    <span
      className={`sketch-pill px-2 py-0.5 text-[11px] font-semibold ${
        on ? 'bg-mint/20 text-mint' : 'bg-paper/70 text-ink/40 line-through'
      }`}
    >
      {label}
    </span>
  )
}
