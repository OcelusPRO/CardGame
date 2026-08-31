import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiError } from '../api/ApiError'
import { gamesApi } from '../api/games'
import { IdentityForm } from '../components/game/IdentityForm'
import { Button } from '../components/ui/Button'
import { Panel } from '../components/ui/Panel'
import { errorMessage } from '../lib/errorMessages'
import { gamePath } from '../lib/gameLinks'
import { useIdentity } from '../session/useIdentity'
import { useSession } from '../session/useSession'

/** Pick a name, build a face, open the table. */
export function CreatePage() {
  const navigate = useNavigate()
  const { me } = useSession()
  const [identity, setIdentity] = useIdentity(me)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const create = async () => {
    setBusy(true)
    setError(null)
    try {
      const ticket = await gamesApi.create(identity.nickname, identity.avatar)
      navigate(gamePath(ticket.code))
    } catch (failure) {
      setError(failure instanceof ApiError ? errorMessage(failure.code) : errorMessage('NETWORK_ERROR'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="mx-auto flex max-w-2xl flex-col gap-5 px-4 py-10">
      <h1 className="font-display text-4xl font-extrabold">Créer une partie</h1>

      <Panel>
        <IdentityForm identity={identity} onChange={setIdentity} me={me} autoFocus />
      </Panel>

      {error && <p className="rounded-2xl bg-red-500/15 px-4 py-3 text-sm text-red-200">{error}</p>}

      <Button full disabled={identity.nickname.trim().length < 2 || busy} onClick={create}>
        {busy ? 'Création…' : 'Ouvrir la table'}
      </Button>
    </div>
  )
}
