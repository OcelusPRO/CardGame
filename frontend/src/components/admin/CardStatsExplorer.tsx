import { useEffect, useMemo, useState } from 'react'
import { adminApi } from '../../api/admin'
import type { CardAdminView, CardStatsView, PackAdminView } from '../../api/adminTypes'
import { StatTile } from './StatTile'

interface Props {
  packs: PackAdminView[]
  punchlines: CardAdminView[]
}

const SHOWN = 40

/**
 * Look up one answer card: pick a pack, search by text, then read its play record —
 * how often it was dealt, played, voted for, and the situation it did best against.
 */
export function CardStatsExplorer({ packs, punchlines }: Props) {
  const [packId, setPackId] = useState('')
  const [query, setQuery] = useState('')
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [stats, setStats] = useState<CardStatsView | null>(null)
  const [loading, setLoading] = useState(false)

  const matches = useMemo(() => {
    const needle = query.trim().toLowerCase()
    return punchlines.filter(
      (card) =>
        (!packId || card.packId === packId) &&
        (!needle || card.text.toLowerCase().includes(needle)),
    )
  }, [punchlines, packId, query])

  // The selected card can fall outside the current filter; drop it when it does.
  useEffect(() => {
    if (selectedId && !matches.some((card) => card.id === selectedId)) {
      setSelectedId(null)
      setStats(null)
    }
  }, [matches, selectedId])

  useEffect(() => {
    if (!selectedId) return
    let live = true
    setLoading(true)
    adminApi
      .cardStats(selectedId)
      .then((data) => live && setStats(data))
      .catch(() => live && setStats(null))
      .finally(() => live && setLoading(false))
    return () => {
      live = false
    }
  }, [selectedId])

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap gap-2">
        <select
          value={packId}
          aria-label="Filtrer par pack"
          onChange={(event) => setPackId(event.target.value)}
          className="rounded-full bg-white/10 px-4 py-2 text-sm outline-none ring-1 ring-white/15"
        >
          <option value="">Tous les packs</option>
          {packs.map((pack) => (
            <option key={pack.id} value={pack.id} className="bg-ink-soft">
              {pack.name}
            </option>
          ))}
        </select>
        <input
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="Rechercher une réponse…"
          className="min-w-48 flex-1 rounded-full bg-white/10 px-4 py-2 text-sm outline-none ring-1 ring-white/15 focus:ring-2 focus:ring-punch"
        />
      </div>

      <ul className="max-h-64 divide-y divide-white/10 overflow-y-auto rounded-2xl bg-white/5">
        {matches.slice(0, SHOWN).map((card) => (
          <li key={card.id}>
            <button
              type="button"
              onClick={() => setSelectedId(card.id)}
              className={`flex w-full items-center gap-3 px-4 py-2 text-left text-sm transition hover:bg-white/5 ${
                selectedId === card.id ? 'bg-punch/20' : ''
              }`}
            >
              <span className="min-w-0 flex-1 truncate">{card.text}</span>
              {!card.enabled && <span className="text-xs text-white/40">désactivée</span>}
            </button>
          </li>
        ))}
        {matches.length === 0 && (
          <li className="px-4 py-3 text-sm text-white/50">Aucune réponse ne correspond.</li>
        )}
        {matches.length > SHOWN && (
          <li className="px-4 py-2 text-xs text-white/40">
            {matches.length - SHOWN} autres… affinez la recherche.
          </li>
        )}
      </ul>

      {selectedId && (
        <div className="flex flex-col gap-4 rounded-2xl bg-white/5 p-4 ring-1 ring-white/10">
          {loading && <p className="text-sm text-white/50">Chargement des statistiques…</p>}
          {!loading && stats && (
            <>
              <p className="font-hand text-2xl leading-tight text-white/90">{stats.text}</p>
              <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
                <StatTile label="Fois en main" value={stats.deals} accent="text-blue-300" />
                <StatTile label="Fois jouée" value={stats.plays} accent="text-grape" />
                <StatTile label="Votes reçus" value={stats.votes} accent="text-zap" />
                <StatTile label="Manches gagnées" value={stats.wins} accent="text-mint" />
              </div>
              <div className="rounded-2xl bg-white/5 p-4">
                <p className="text-xs font-semibold uppercase tracking-wider text-white/50">
                  Meilleure situation avec cette réponse
                </p>
                {stats.bestSituation ? (
                  <>
                    <p className="mt-1 font-hand text-xl leading-tight text-white/90">
                      {stats.bestSituation.text}
                    </p>
                    <p className="mt-2 text-sm text-white/60">
                      {stats.bestSituation.votes} vote(s) · {stats.bestSituation.wins} victoire(s) ·{' '}
                      {stats.bestSituation.plays} fois jouée ensemble
                    </p>
                  </>
                ) : (
                  <p className="mt-1 text-sm text-white/50">Jamais jouée en situation pour l&apos;instant.</p>
                )}
              </div>
            </>
          )}
          {!loading && !stats && (
            <p className="text-sm text-white/50">Statistiques indisponibles pour cette carte.</p>
          )}
        </div>
      )}
    </div>
  )
}
