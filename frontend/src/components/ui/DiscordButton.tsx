import type { MeView } from '../../api/types'

interface Props {
  me: MeView | null
}

/**
 * Optional Discord sign in. It is a plain link because the OAuth dance is a full page
 * redirect, and it simply disappears when the server has no Discord credentials.
 */
export function DiscordButton({ me }: Props) {
  if (!me?.discordLoginAvailable) return null

  if (me.discordConnected) {
    return (
      <span className="flex items-center gap-2 rounded-full bg-white/10 px-3 py-1.5 text-sm font-semibold">
        {me.discordAvatarUrl && (
          <img src={me.discordAvatarUrl} alt="" className="size-6 rounded-full" />
        )}
        {me.discordUsername}
      </span>
    )
  }

  return (
    <a
      href="/auth/discord"
      className="rounded-full bg-[#5865F2] px-4 py-2 text-sm font-bold text-white transition hover:brightness-110"
    >
      Connexion Discord
    </a>
  )
}
