import { useEffect, useRef, useState } from 'react'
import { AnimatePresence, motion } from 'motion/react'
import { Link } from 'react-router-dom'
import { sessionApi } from '../../api/session'
import type { MeView } from '../../api/types'
import { SignInDialog, type SignInProvider } from './SignInDialog'

interface Props {
  me: MeView | null
}

/**
 * Optional sign in. Signed out it is a single button opening the choice of accounts —
 * Discord or Twitch, one or the other. Signed in the pill becomes a menu: the admin
 * shortcut when the account carries it, and a way back out. It disappears entirely when
 * the server has credentials for neither.
 */
export function AccountMenu({ me }: Props) {
  const [open, setOpen] = useState(false)
  const [signingIn, setSigningIn] = useState(false)
  const [busy, setBusy] = useState(false)
  const rootRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!open) return
    const onPointer = (event: MouseEvent) => {
      if (rootRef.current && !rootRef.current.contains(event.target as Node)) setOpen(false)
    }
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setOpen(false)
    }
    document.addEventListener('mousedown', onPointer)
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('mousedown', onPointer)
      document.removeEventListener('keydown', onKey)
    }
  }, [open])

  if (!me) return null

  const offered = providersOf(me)
  const signedIn = me.discordConnected || me.twitchConnected

  if (!signedIn) {
    return offered.length === 0 ? null : (
      <>
        <button
          type="button"
          onClick={() => setSigningIn(true)}
          className="sketch-pill bg-grape px-4 py-2 text-sm font-bold text-white transition hover:brightness-110"
        >
          Se connecter
        </button>
        <AnimatePresence>
          {signingIn && <SignInDialog providers={offered} onClose={() => setSigningIn(false)} />}
        </AnimatePresence>
      </>
    )
  }

  const logout = async () => {
    setBusy(true)
    try {
      await sessionApi.logout()
    } catch {
      // The cookie is cleared server-side on a best effort; reload regardless so no
      // screen keeps showing a stale identity.
    }
    window.location.href = '/'
  }

  return (
    <div ref={rootRef} className="relative">
      <button
        type="button"
        onClick={() => setOpen((value) => !value)}
        aria-haspopup="menu"
        aria-expanded={open}
        className="sketch-pill flex items-center gap-2 bg-paper px-3 py-1.5 text-sm font-semibold transition hover:bg-ink/8"
      >
        {pictureOf(me) && <img src={pictureOf(me)} alt="" className="size-6 rounded-full" />}
        {nameOf(me)}
        <svg
          viewBox="0 0 24 24"
          aria-hidden="true"
          className={`size-3.5 text-ink/50 transition-transform ${open ? 'rotate-180' : ''}`}
        >
          <path d="M6 9l6 6 6-6" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      </button>

      <AnimatePresence>
        {open && (
          <motion.div
            role="menu"
            initial={{ opacity: 0, y: -6, scale: 0.97 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -6, scale: 0.97 }}
            transition={{ type: 'spring', stiffness: 500, damping: 30 }}
            /* Positioning lives here, on a plain element: `.sketch` is unlayered and would
               force `position: relative`, which drops the menu back into the header flow. */
            className="absolute right-0 top-full z-30 mt-2 min-w-44 origin-top-right"
          >
            <div className="sketch flex flex-col gap-1 bg-paper p-2 shadow-card">
              {me.isAdmin && (
                <Link
                  to="/admin"
                  role="menuitem"
                  onClick={() => setOpen(false)}
                  className="rounded-lg px-3 py-2 text-left text-sm font-semibold transition hover:bg-ink/8"
                >
                  Espace administration
                </Link>
              )}
              <button
                type="button"
                role="menuitem"
                onClick={logout}
                disabled={busy}
                className="rounded-lg px-3 py-2 text-left text-sm font-semibold text-punch transition hover:bg-punch/10 disabled:opacity-50"
              >
                {busy ? 'Déconnexion…' : 'Se déconnecter'}
              </button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}

/** The accounts this server can actually sign somebody in with. */
function providersOf(me: MeView): SignInProvider[] {
  const providers: SignInProvider[] = []
  if (me.discordLoginAvailable) {
    providers.push({
      id: 'discord',
      label: 'Discord',
      href: '/auth/discord',
      colour: 'bg-[#5865F2]',
    })
  }
  if (me.twitchLoginAvailable) {
    providers.push({
      id: 'twitch',
      label: 'Twitch',
      href: '/auth/twitch',
      colour: 'bg-[#9146FF]',
    })
  }
  return providers
}

/** Whichever name the player gave us, Discord first because it came first. */
function nameOf(me: MeView): string {
  return me.discordUsername ?? me.twitchUsername ?? 'Vous'
}

function pictureOf(me: MeView): string | undefined {
  return me.discordAvatarUrl ?? me.twitchAvatarUrl
}
