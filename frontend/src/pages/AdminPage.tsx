import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { adminApi } from '../api/admin'
import type { AdminOverview, ComboView, DailyActivityView } from '../api/adminTypes'
import { ActivityChart } from '../components/admin/ActivityChart'
import { CardEditor } from '../components/admin/CardEditor'
import { CardStatsExplorer } from '../components/admin/CardStatsExplorer'
import { ComboTable } from '../components/admin/ComboTable'
import { DeckTransfer } from '../components/admin/DeckTransfer'
import { PackEditor } from '../components/admin/PackEditor'
import { StatTile } from '../components/admin/StatTile'
import { useAdminCatalog } from '../components/admin/useAdminCatalog'
import { useLiveStats } from '../components/admin/useLiveStats'
import { Panel } from '../components/ui/Panel'
import { useSession } from '../session/useSession'

/** Dashboard and card editor, reachable only by the allowlisted Discord accounts. */
export function AdminPage() {
  const { me } = useSession()
  const live = useLiveStats()
  const catalog = useAdminCatalog()
  const [overview, setOverview] = useState<AdminOverview | null>(null)
  const [activity, setActivity] = useState<DailyActivityView[]>([])
  const [combos, setCombos] = useState<ComboView[]>([])

  useEffect(() => {
    adminApi.overview().then(setOverview).catch(() => setOverview(null))
    adminApi.activity(30).then(setActivity).catch(() => setActivity([]))
    adminApi.combos().then(setCombos).catch(() => setCombos([]))
  }, [])

  if (me && !me.isAdmin) {
    return (
      <div className="mx-auto max-w-md px-4 py-24 text-center">
        <p className="font-display text-2xl">Espace réservé</p>
        <p className="mt-2 text-ink/70">Connectez-vous avec un compte Discord administrateur.</p>
        <Link to="/" className="mt-4 inline-block text-sm underline text-ink/60">
          Retour à l'accueil
        </Link>
      </div>
    )
  }

  return (
    <div className="mx-auto flex max-w-6xl flex-col gap-6 px-4 py-8">
      <h1 className="font-display text-4xl font-extrabold">Administration</h1>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatTile label="Parties en cours" value={live?.activeGames ?? overview?.live.activeGames ?? 0} accent="text-punch" />
        <StatTile label="Joueurs connectés" value={live?.connectedPlayers ?? overview?.live.connectedPlayers ?? 0} accent="text-mint" />
        <StatTile label="Situations" value={overview?.situations ?? 0} />
        <StatTile label="Réponses" value={overview?.punchlines ?? 0} />
      </div>

      <Panel title="Activité des 30 derniers jours">
        <ActivityChart data={activity} />
      </Panel>

      <Panel title="Statistiques d'une réponse">
        <CardStatsExplorer packs={catalog.packs} punchlines={catalog.punchlines} />
      </Panel>

      <Panel title="Meilleurs duos situation / réponse">
        <ComboTable combos={combos} />
      </Panel>

      <Panel title="Packs">
        <PackEditor packs={catalog.packs} onSave={catalog.savePack} onDelete={catalog.deletePack} />
      </Panel>

      <Panel title="Import / export de deck">
        <DeckTransfer
          packs={catalog.packs}
          situations={catalog.situations}
          punchlines={catalog.punchlines}
          onImport={catalog.importPack}
        />
      </Panel>

      <Panel title="Cartes officielles">
        <div className="flex flex-col gap-8">
          <CardEditor
            title="Situations"
            hint="Utilisez ____ pour marquer chaque trou à remplir."
            cards={catalog.situations}
            packs={catalog.packs}
            onSave={catalog.saveSituation}
            onDelete={catalog.deleteSituation}
          />
          <CardEditor
            title="Réponses"
            hint="Une phrase courte, sans point final. Utilisez ____ pour un trou que le joueur complétera."
            cards={catalog.punchlines}
            packs={catalog.packs}
            onSave={catalog.savePunchline}
            onDelete={catalog.deletePunchline}
          />
        </div>
      </Panel>
    </div>
  )
}
