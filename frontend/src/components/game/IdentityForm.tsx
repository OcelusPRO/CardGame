import type { Identity } from '../../lib/identity'
import type { MeView } from '../../api/types'
import { AvatarBuilder } from '../avatar/AvatarBuilder'
import { TextField } from '../ui/TextField'

interface Props {
  identity: Identity
  onChange: (identity: Identity) => void
  me: MeView | null
  autoFocus?: boolean
}

/** Pseudo plus avatar, the two things every player picks before sitting down. */
export function IdentityForm({ identity, onChange, me, autoFocus = false }: Props) {
  return (
    <div className="flex flex-col gap-5">
      <TextField
        label="Votre pseudo"
        value={identity.nickname}
        onChange={(nickname) => onChange({ ...identity, nickname })}
        placeholder="Jean-Michel"
        maxLength={20}
        autoFocus={autoFocus}
      />
      <AvatarBuilder
        value={identity.avatar}
        pictureUrl={me?.discordAvatarUrl ?? me?.twitchAvatarUrl}
        onChange={(avatar) => onChange({ ...identity, avatar })}
      />
    </div>
  )
}
