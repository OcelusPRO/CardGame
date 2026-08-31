import { useState } from 'react'
import { ApiError } from '../../api/ApiError'
import { gamesApi } from '../../api/games'
import type { GamePreview, MeView } from '../../api/types'
import { errorMessage } from '../../lib/errorMessages'
import type { Identity } from '../../lib/identity'
import { Button } from '../ui/Button'
import { Panel } from '../ui/Panel'
import { IdentityForm } from './IdentityForm'

interface Props {
  preview: GamePreview
  identity: Identity
  onIdentityChange: (identity: Identity) => void
  me: MeView | null
  onJoined: () => void
}

/**
 * What a newcomer sees at the game address. The link to a table is the table itself, so
 * this form lives on the same page rather than behind a separate join screen.
 */
export function GameJoinCard({ preview, identity, onIdentityChange, me, onJoined }: Props) {
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const join = async () => {
    setBusy(true)
    setError(null)
    try {
      await gamesApi.join(preview.code, identity.nickname, identity.avatar)
      onJoined()
    } catch (failure) {
      setError(failure instanceof ApiError ? errorMessage(failure.code) : errorMessage('NETWORK_ERROR'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="mx-auto flex w-full max-w-2xl flex-col gap-5 px-4 py-10">
      <div>
        <h1 className="font-display text-4xl font-extrabold">Rejoindre la partie</h1>
        <p className="mt-1 text-white/60">
          Table de {preview.hostNickname} · {preview.playerCount}/{preview.maxPlayers} joueurs
        </p>
      </div>

      {!preview.canJoin && (
        <p className="rounded-2xl bg-zap/15 px-4 py-3 text-sm text-zap">
          {preview.phase === 'LOBBY'
            ? 'La table est complète.'
            : 'La partie a déjà commencé : impossible de prendre une place.'}
        </p>
      )}

      <Panel>
        <IdentityForm identity={identity} onChange={onIdentityChange} me={me} autoFocus />
      </Panel>

      {error && <p className="rounded-2xl bg-red-500/15 px-4 py-3 text-sm text-red-200">{error}</p>}

      <Button
        full
        disabled={!preview.canJoin || identity.nickname.trim().length < 2 || busy}
        onClick={join}
      >
        {busy ? 'Connexion…' : 'Prendre une place'}
      </Button>
    </div>
  )
}
