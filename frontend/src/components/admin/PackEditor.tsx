import { useState } from 'react'
import type { PackAdminView } from '../../api/adminTypes'
import { Button } from '../ui/Button'

interface Props {
  packs: PackAdminView[]
  onSave: (
    name: string,
    description: string,
    answerModeCards: boolean,
    answerModeFreeText: boolean,
  ) => void
  onDelete: (id: string) => void
}

/** Creating and dropping the themed packs the cards belong to. */
export function PackEditor({ packs, onSave, onDelete }: Props) {
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [cards, setCards] = useState(true)
  const [freeText, setFreeText] = useState(true)

  const reset = () => {
    setName('')
    setDescription('')
    setCards(true)
    setFreeText(true)
  }

  return (
    <div className="flex flex-col gap-4">
      <ul className="flex flex-wrap gap-2">
        {packs.map((pack) => (
          <li key={pack.id} className="flex items-center gap-3 rounded-2xl bg-white/10 px-3 py-2 text-sm">
            <span>
              <span className="block font-semibold">{pack.name}</span>
              <span className="block text-xs text-white/50">
                {pack.situationCount} situations · {pack.punchlineCount} réponses
                {pack.enabled ? '' : ' · désactivé'}
                {modeLabel(pack)}
              </span>
            </span>
            <button
              type="button"
              aria-label={`Supprimer ${pack.name}`}
              onClick={() => onDelete(pack.id)}
              className="text-white/40 transition hover:text-red-300"
            >
              ✕
            </button>
          </li>
        ))}
        {packs.length === 0 && <li className="text-sm text-white/50">Aucun pack pour l&apos;instant.</li>}
      </ul>

      <div className="flex flex-col gap-3">
        <div className="flex flex-wrap gap-2">
          <input
            value={name}
            onChange={(event) => setName(event.target.value)}
            placeholder="Nom du pack"
            className="flex-1 rounded-full bg-white/10 px-4 py-2 text-sm outline-none ring-1 ring-white/15 focus:ring-2 focus:ring-punch"
          />
          <input
            value={description}
            onChange={(event) => setDescription(event.target.value)}
            placeholder="Description"
            className="flex-1 rounded-full bg-white/10 px-4 py-2 text-sm outline-none ring-1 ring-white/15 focus:ring-2 focus:ring-punch"
          />
        </div>

        <fieldset className="flex flex-wrap items-center gap-4 text-sm">
          <legend className="mb-1 text-xs font-semibold uppercase tracking-wider text-white/50">
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

        <div>
          <Button
            variant="zap"
            disabled={!name.trim() || (!cards && !freeText)}
            onClick={() => {
              onSave(name.trim(), description.trim(), cards, freeText)
              reset()
            }}
          >
            Ajouter
          </Button>
          {!cards && !freeText && (
            <p className="mt-1 text-xs text-zap">Choisissez au moins un mode de jeu.</p>
          )}
        </div>
      </div>
    </div>
  )
}

/** Only worth showing when the pack is actually restricted to one mode. */
function modeLabel(pack: PackAdminView): string {
  if (pack.answerModeCards && pack.answerModeFreeText) return ''
  if (pack.answerModeCards) return ' · mode cartes uniquement'
  if (pack.answerModeFreeText) return ' · mode sans limites uniquement'
  return ' · aucun mode'
}
