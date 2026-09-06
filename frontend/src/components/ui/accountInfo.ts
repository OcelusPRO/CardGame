import type { MeView } from '../../api/types'
import type { SignInProvider } from './SignInDialog'

/** The accounts this server can actually sign somebody in with. */
export function providersOf(me: MeView): SignInProvider[] {
  const providers: SignInProvider[] = []
  if (me.discordLoginAvailable) {
    providers.push({ id: 'discord', label: 'Discord', href: '/auth/discord', colour: 'bg-[#5865F2]' })
  }
  if (me.twitchLoginAvailable) {
    providers.push({ id: 'twitch', label: 'Twitch', href: '/auth/twitch', colour: 'bg-[#9146FF]' })
  }
  return providers
}

/** Whichever name the player gave us, Discord first because it came first. */
export function nameOf(me: MeView): string {
  return me.discordUsername ?? me.twitchUsername ?? 'Vous'
}

export function pictureOf(me: MeView): string | undefined {
  return me.discordAvatarUrl ?? me.twitchAvatarUrl
}

export function isSignedIn(me: MeView): boolean {
  return me.discordConnected || me.twitchConnected
}

/** Clears the server cookie on a best effort, then reloads so no screen keeps a stale identity. */
export async function signOut(logout: () => Promise<unknown>): Promise<void> {
  try {
    await logout()
  } catch {
    // Best effort: reload regardless.
  }
  window.location.href = '/'
}
