import { renderHook, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it } from 'vitest'
import type { MeView } from '../api/types'
import { IDENTITY_KEY } from '../lib/identity'
import { useIdentity } from './useIdentity'

function discordUser(username: string): MeView {
  return {
    playerId: 'p1',
    discordConnected: true,
    discordUsername: username,
    twitchConnected: false,
    isAdmin: false,
    discordLoginAvailable: true,
    twitchLoginAvailable: true,
  }
}

function twitchUser(username: string): MeView {
  return {
    playerId: 'p1',
    discordConnected: false,
    twitchConnected: true,
    twitchUsername: username,
    twitchLogin: username.toLowerCase(),
    isAdmin: false,
    discordLoginAvailable: true,
    twitchLoginAvailable: true,
  }
}

const anonymous: MeView = {
  playerId: 'p1',
  discordConnected: false,
  twitchConnected: false,
  isAdmin: false,
  discordLoginAvailable: true,
  twitchLoginAvailable: true,
}

describe('useIdentity', () => {
  beforeEach(() => window.localStorage.clear())

  it('suggests the Discord name to a player who has none yet', async () => {
    const { result } = renderHook(() => useIdentity(discordUser('Jean-Michel')))

    await waitFor(() => expect(result.current[0].nickname).toBe('Jean-Michel'))
  })

  it('suggests the Twitch name when there is no Discord one', async () => {
    const { result } = renderHook(() => useIdentity(twitchUser('Kameto')))

    await waitFor(() => expect(result.current[0].nickname).toBe('Kameto'))
  })

  it('never overwrites a name the player already chose', async () => {
    window.localStorage.setItem(
      IDENTITY_KEY,
      JSON.stringify({ nickname: 'Bibi', avatar: { topStyleId: 'head-round', topColor: '#ffffff', bottomStyleId: 'body-tee', bottomColor: '#000000' } }),
    )

    const { result } = renderHook(() => useIdentity(discordUser('Jean-Michel')))

    await waitFor(() => expect(result.current[0].nickname).toBe('Bibi'))
  })

  it('leaves the name empty without Discord', async () => {
    const { result } = renderHook(() => useIdentity(anonymous))

    await waitFor(() => expect(result.current[0].nickname).toBe(''))
  })

  it('keeps the suggestion editable', async () => {
    const { result } = renderHook(() => useIdentity(discordUser('Jean-Michel')))
    await waitFor(() => expect(result.current[0].nickname).toBe('Jean-Michel'))

    result.current[1]({ ...result.current[0], nickname: 'Autre chose' })

    await waitFor(() => expect(result.current[0].nickname).toBe('Autre chose'))
  })
})
