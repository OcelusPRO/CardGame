import { useEffect, useRef, useState } from 'react'
import { AnimatePresence, motion } from 'motion/react'
import { Link } from 'react-router-dom'
import { sessionApi } from '../../api/session'
import type { MeView } from '../../api/types'
import { AnimationToggle } from './AnimationToggle'
import { SoundToggle } from './SoundToggle'
import { ThemeToggle } from './ThemeToggle'
import { SignInDialog } from './SignInDialog'
import { isSignedIn, nameOf, pictureOf, providersOf, signOut } from './accountInfo'

interface Props {
  me: MeView | null
  sound: boolean
  onToggleSound: () => void
  dark: boolean
  onToggleTheme: () => void
  animate: boolean
  onToggleAnimations: () => void
}

/**
 * The header, folded up for a phone. Four separate controls eat the width the site name
 * needs, so on a small screen they all move behind one trigger: a burger while nobody is
 * signed in, and the player's profile picture in a circle once somebody is — an avatar
 * says "this is your account" faster than any icon.
 */
export function HeaderMenu({
  me,
  sound,
  onToggleSound,
  dark,
  onToggleTheme,
  animate,
  onToggleAnimations,
}: Props) {
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

  const signedIn = me != null && isSignedIn(me)
  const offered = me ? providersOf(me) : []
  const picture = me && signedIn ? pictureOf(me) : undefined

  const logout = async () => {
    setBusy(true)
    await signOut(sessionApi.logout)
  }

  return (
    <div ref={rootRef} className="relative">
      <button
        type="button"
        onClick={() => setOpen((value) => !value)}
        aria-haspopup="menu"
        aria-expanded={open}
        aria-label={signedIn ? 'Mon compte et réglages' : 'Menu'}
        className="sketch-pill flex size-9 shrink-0 items-center justify-center overflow-hidden bg-paper text-ink/70 transition hover:bg-ink/8"
      >
        {signedIn ? (
          picture ? (
            <img src={picture} alt="" className="size-8 rounded-full object-cover" />
          ) : (
            <span className="flex size-8 items-center justify-center rounded-full bg-grape text-sm font-black text-white">
              {nameOf(me!).slice(0, 1).toUpperCase()}
            </span>
          )
        ) : (
          <svg viewBox="0 0 24 24" aria-hidden="true" className="size-4" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
            <path d="M4 7h16M4 12h16M4 17h16" />
          </svg>
        )}
      </button>

      <AnimatePresence>
        {open && (
          <motion.div
            role="menu"
            initial={{ opacity: 0, y: -6, scale: 0.97 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -6, scale: 0.97 }}
            transition={{ type: 'spring', stiffness: 500, damping: 30 }}
            /* Positioning stays on a plain element: `.sketch` is unlayered and would force
               `position: relative`, dropping the menu back into the header flow. */
            className="absolute right-0 top-full z-30 mt-2 min-w-56 origin-top-right"
          >
            <div className="sketch flex flex-col gap-1 bg-paper p-2 shadow-card">
              {signedIn && (
                <p className="truncate px-3 py-1 text-sm font-black">{nameOf(me!)}</p>
              )}

              <SettingRow label="Sons">
                <SoundToggle enabled={sound} onToggle={onToggleSound} />
              </SettingRow>
              <SettingRow label="Thème">
                <ThemeToggle dark={dark} onToggle={onToggleTheme} />
              </SettingRow>
              <SettingRow label="Animations">
                <AnimationToggle enabled={animate} onToggle={onToggleAnimations} />
              </SettingRow>

              {me && (signedIn || offered.length > 0) && <hr className="my-1 border-ink/15" />}

              {signedIn && me?.isAdmin && (
                <Link
                  to="/admin"
                  role="menuitem"
                  onClick={() => setOpen(false)}
                  className="rounded-lg px-3 py-2 text-left text-sm font-semibold transition hover:bg-ink/8"
                >
                  Espace administration
                </Link>
              )}
              {signedIn ? (
                <button
                  type="button"
                  role="menuitem"
                  onClick={logout}
                  disabled={busy}
                  className="rounded-lg px-3 py-2 text-left text-sm font-semibold text-punch transition hover:bg-punch/10 disabled:opacity-50"
                >
                  {busy ? 'Déconnexion…' : 'Se déconnecter'}
                </button>
              ) : (
                offered.length > 0 && (
                  <button
                    type="button"
                    role="menuitem"
                    onClick={() => {
                      setOpen(false)
                      setSigningIn(true)
                    }}
                    className="rounded-lg px-3 py-2 text-left text-sm font-semibold transition hover:bg-ink/8"
                  >
                    Se connecter
                  </button>
                )
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      <AnimatePresence>
        {signingIn && <SignInDialog providers={offered} onClose={() => setSigningIn(false)} />}
      </AnimatePresence>
    </div>
  )
}

/** A named line in the menu: what the switch does on the left, the switch itself on the right. */
function SettingRow({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex items-center justify-between gap-4 rounded-lg px-3 py-1.5 text-sm font-semibold">
      <span>{label}</span>
      {children}
    </div>
  )
}
