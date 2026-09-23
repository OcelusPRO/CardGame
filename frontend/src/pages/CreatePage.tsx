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

/**
 * Pick a name, build a face, open the table — online, one device per player, or on this
 * device alone, passed round the room. The second is the salon game: the host adds the
 * other players by name in the lobby, and nobody else needs a phone.
 */
export function CreatePage() {
  const navigate = useNavigate()
  const { me } = useSession()
  const [identity, setIdentity] = useIdentity(me)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [sharedDevice, setSharedDevice] = useState(false)

  const create = async () => {
    setBusy(true)
    setError(null)
    try {
      const ticket = await gamesApi.create(identity.nickname, identity.avatar, undefined, sharedDevice)
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

      <Panel>
        <fieldset>
          <legend className="mb-3 text-xs font-semibold uppercase tracking-wider text-ink/60">
            Comment jouez-vous&nbsp;?
          </legend>
          <div className="grid gap-3 sm:grid-cols-2">
            <ModeOption
              selected={!sharedDevice}
              onSelect={() => setSharedDevice(false)}
              title="En ligne"
              description="Chacun joue sur son propre appareil, avec le lien d'invitation."
            />
            <ModeOption
              selected={sharedDevice}
              onSelect={() => setSharedDevice(true)}
              title="Sur cet appareil"
              description="Mode salon : on se passe le téléphone, chacun son tour, sans regarder la main des autres."
            />
          </div>
        </fieldset>
      </Panel>

      {error && <p className="sketch [--stroke:#dc2626] bg-red-500/15 px-4 py-3 text-sm text-red-200">{error}</p>}

      <Button full disabled={identity.nickname.trim().length < 2 || busy} onClick={create}>
        {busy ? 'Création…' : 'Ouvrir la table'}
      </Button>
    </div>
  )
}

interface ModeOptionProps {
  selected: boolean
  onSelect: () => void
  title: string
  description: string
}

function ModeOption({ selected, onSelect, title, description }: ModeOptionProps) {
  return (
    <button
      type="button"
      aria-pressed={selected}
      onClick={onSelect}
      className={`sketch-alt flex flex-col gap-1 px-4 py-3 text-left transition ${
        selected ? 'bg-mint/20 ring-2 ring-mint' : 'bg-paper/70 hover:bg-ink/5'
      }`}
    >
      <span className="font-display text-lg font-bold">{title}</span>
      <span className="text-sm text-ink/65">{description}</span>
    </button>
  )
}
