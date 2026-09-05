import { useEffect, useState } from 'react'
import { adminApi } from '../../api/admin'
import { ApiError } from '../../api/ApiError'
import type { AccountProvider, AccountView, AdultAccessView } from '../../api/adminTypes'
import { errorMessage } from '../../lib/errorMessages'
import { Button } from '../ui/Button'

const PROVIDERS: { value: AccountProvider; label: string; hint: string }[] = [
  { value: 'DISCORD', label: 'Discord', hint: '123456789012345678' },
  { value: 'TWITCH', label: 'Twitch', hint: 'kameto — ou son identifiant' },
]

/** Long enough not to query the server on the first keystroke. */
const LOOKUP_DELAY_MS = 400

/**
 * The allowlist of accounts cleared to see and pick the packs marked "interdit aux
 * mineurs". Administrators are always cleared and are not listed here, and so is any
 * account old enough for the age rule.
 *
 * Nobody remembers an account id, so the pseudo is looked up while it is typed — and on
 * Twitch a channel name is enough: the server turns it into the id it stores.
 */
export function AdultAccessEditor() {
  const [entries, setEntries] = useState<AdultAccessView[]>([])
  const [provider, setProvider] = useState<AccountProvider>('DISCORD')
  const [query, setQuery] = useState('')
  const [label, setLabel] = useState('')
  const [found, setFound] = useState<AccountView | null>(null)
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

  // Who is behind what is being typed. An unknown account is not an error here: the
  // administrator may simply not have finished typing.
  useEffect(() => {
    const asked = query.trim()
    if (asked.length < 2) {
      setFound(null)
      return
    }
    let cancelled = false
    const timer = setTimeout(() => {
      adminApi
        .findAccount(provider, asked)
        .then((account) => {
          if (!cancelled) setFound(account)
        })
        .catch(() => {
          if (!cancelled) setFound(null)
        })
    }, LOOKUP_DELAY_MS)
    return () => {
      cancelled = true
      clearTimeout(timer)
    }
  }, [provider, query])

  const add = async () => {
    setError(null)
    try {
      await adminApi.addAdultAccess({ provider, accountId: query.trim(), label: label.trim() })
      setQuery('')
      setLabel('')
      setFound(null)
      reload()
    } catch (failure) {
      setError(failure instanceof ApiError ? errorMessage(failure.code) : "L'ajout a échoué.")
    }
  }

  const remove = async (entry: AdultAccessView) => {
    setError(null)
    try {
      await adminApi.removeAdultAccess(entry.provider, entry.accountId)
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
        Ces comptes Discord ou Twitch peuvent voir et sélectionner les packs 18+ quand ils sont
        hôtes. Les administrateurs y ont accès d&apos;office, tout comme les comptes de plus de
        trois ans.
      </p>

      <ul className="flex flex-col gap-2">
        {entries.map((entry) => (
          <li
            key={`${entry.provider}:${entry.accountId}`}
            className="flex items-center gap-3 sketch-alt bg-ink/5 px-3 py-2 text-sm"
          >
            <span className="min-w-0 flex-1">
              <span className="block font-semibold">{entry.label || 'Sans nom'}</span>
              <span className="block font-mono text-xs text-ink/60">
                {providerLabel(entry.provider)} · {entry.accountId}
              </span>
            </span>
            <button
              type="button"
              aria-label={`Retirer ${entry.label || entry.accountId}`}
              onClick={() => remove(entry)}
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

      <div className="sketch flex flex-col gap-3 bg-paper/70 p-3">
        <div className="flex flex-wrap items-end gap-2">
          <fieldset className="flex flex-col gap-1">
            <legend className="mb-1 text-xs font-semibold uppercase tracking-wider text-ink/60">
              Compte
            </legend>
            <div className="flex gap-2">
              {PROVIDERS.map((option) => (
                <button
                  key={option.value}
                  type="button"
                  aria-pressed={provider === option.value}
                  onClick={() => setProvider(option.value)}
                  className={`sketch-pill px-3 py-2 text-sm font-semibold transition ${
                    provider === option.value ? 'bg-grape text-white' : 'bg-ink/5 hover:bg-ink/10'
                  }`}
                >
                  {option.label}
                </button>
              ))}
            </div>
          </fieldset>
          <label className="flex flex-1 flex-col gap-1 text-xs font-semibold uppercase tracking-wider text-ink/60">
            {provider === 'TWITCH' ? 'Chaîne ou identifiant' : 'Identifiant numérique'}
            <input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder={PROVIDERS.find((option) => option.value === provider)?.hint}
              className="sketch-input bg-paper px-4 py-2 font-mono text-sm normal-case outline-none focus:border-punch"
            />
          </label>
          <label className="flex flex-1 flex-col gap-1 text-xs font-semibold uppercase tracking-wider text-ink/60">
            Nom (facultatif)
            <input
              value={label}
              onChange={(event) => setLabel(event.target.value)}
              placeholder={found?.name ?? 'Alex'}
              className="sketch-input bg-paper px-4 py-2 text-sm normal-case outline-none focus:border-punch"
            />
          </label>
          <Button variant="zap" disabled={!query.trim()} onClick={add}>
            Ajouter
          </Button>
        </div>

        {found && (
          <p className="flex items-center gap-2 text-sm text-ink/70">
            {found.avatarUrl && (
              <img src={found.avatarUrl} alt={found.name} className="size-6 rounded-full" />
            )}
            <span>
              <span className="font-semibold">{found.name}</span>{' '}
              <span className="font-mono text-xs text-ink/50">{found.accountId}</span>
            </span>
          </p>
        )}
      </div>
    </div>
  )
}

function providerLabel(provider: AccountProvider): string {
  return provider === 'TWITCH' ? 'Twitch' : 'Discord'
}
