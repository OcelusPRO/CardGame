import { useEffect, useRef } from 'react'
import type { MeView } from '../api/types'
import { useLocalStorage } from '../hooks/useLocalStorage'
import { EMPTY_IDENTITY, IDENTITY_KEY, type Identity } from '../lib/identity'

/**
 * The pseudo and the avatar the browser remembers. Signing in — with Discord, or with
 * Twitch — fills the pseudo in once, as a suggestion: the player stays free to type
 * something else, and a name they already chose is never overwritten.
 */
export function useIdentity(me: MeView | null): [Identity, (identity: Identity) => void] {
  const [identity, setIdentity] = useLocalStorage<Identity>(IDENTITY_KEY, EMPTY_IDENTITY)
  const suggested = useRef(false)

  useEffect(() => {
    if (suggested.current) return
    const signedInName = me?.discordUsername ?? me?.twitchUsername
    if (!signedInName || identity.nickname.trim().length > 0) return
    suggested.current = true
    setIdentity({ ...identity, nickname: signedInName })
  }, [me, identity, setIdentity])

  return [identity, setIdentity]
}
