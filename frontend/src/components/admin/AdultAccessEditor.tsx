import { useEffect, useState } from 'react'
import { adminApi } from '../../api/admin'
import { ApiError } from '../../api/ApiError'
import type { AdultAccessView } from '../../api/adminTypes'
import { errorMessage } from '../../lib/errorMessages'
import { Button } from '../ui/Button'

/**
 * The allowlist of Discord accounts cleared to see and pick the packs marked
 * "interdit aux mineurs". Administrators are always cleared and are not listed here.
 */
export function AdultAccessEditor() {
  const [entries, setEntries] = useState<AdultAccessView[]>([])
  const [discordId, setDiscordId] = useState('')
  const [label, setLabel] = useState('')
  const [error, setError] = useState<string | null>(null)

  const reload = () => {
    adminApi
      .adultAccess()
      .then(setEntries)
      .catch(() => setEntries([]))
  }

  useEffect(() => {
    reload()
  }, [])

  const add = async () => {
    setError(null)
    try {
      await adminApi.addAdultAccess({ discordId: discordId.trim(), label: label.trim() })
      setDiscordId('')
      setLabel('')
      reload()
    } catch (failure) {
      setError(
        failure instanceof ApiError ? errorMessage(failure.code) : "L'ajout a échoué.",
      )
    }
  }

  const remove = async (id: string) => {
    setError(null)
    try {
      await adminApi.removeAdultAccess(id)
      reload()
    } catch (failure) {
      setError(
        failure instanceof ApiError ? errorMessage(failure.code) : 'La suppression a échoué.',
      )
    }
  }

  return (
    <div className="flex flex-col gap-4">
      <p className="text-xs text-ink/60">
        Ces comptes Discord peuvent voir et sélectionner les packs 18+ quand ils sont hôtes.
        Les administrateurs y ont accès d&apos;office.
      </p>

      <ul className="flex flex-col gap-2">
        {entries.map((entry) => (
          <li
            key={entry.discordId}
            className="flex items-center gap-3 sketch-alt bg-ink/5 px-3 py-2 text-sm"
          >
            <span className="min-w-0 flex-1">
              <span className="block font-semibold">{entry.label || 'Sans nom'}</span>
              <span className="block font-mono text-xs text-ink/60">{entry.discordId}</span>
            </span>
            <button
              type="button"
              aria-label={`Retirer ${entry.label || entry.discordId}`}
              onClick={() => remove(entry.discordId)}
              className="text-ink/50 transition hover:text-red-300"
            >
              ✕
            </button>
          </li>
        ))}
        {entries.length === 0 && (
          <li className="text-sm text-ink/60">Personne pour l&apos;instant.</li>
        )}
      </ul>

      {error && <p className="text-sm text-red-300">{error}</p>}

      <div className="sketch flex flex-wrap items-end gap-2 bg-paper/70 p-3">
        <label className="flex flex-1 flex-col gap-1 text-xs font-semibold uppercase tracking-wider text-ink/60">
          Identifiant Discord
          <input
            value={discordId}
            onChange={(event) => setDiscordId(event.target.value)}
            placeholder="123456789012345678"
            inputMode="numeric"
            className="sketch-input bg-paper px-4 py-2 font-mono text-sm normal-case outline-none focus:border-punch"
          />
        </label>
        <label className="flex flex-1 flex-col gap-1 text-xs font-semibold uppercase tracking-wider text-ink/60">
          Nom (facultatif)
          <input
            value={label}
            onChange={(event) => setLabel(event.target.value)}
            placeholder="Alex"
            className="sketch-input bg-paper px-4 py-2 text-sm normal-case outline-none focus:border-punch"
          />
        </label>
        <Button variant="zap" disabled={!discordId.trim()} onClick={add}>
          Ajouter
        </Button>
      </div>
    </div>
  )
}
