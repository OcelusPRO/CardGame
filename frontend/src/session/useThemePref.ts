import { useCallback, useEffect, useState } from 'react'
import { useLocalStorage } from '../hooks/useLocalStorage'

/** `system` follows the OS appearance; the others are a deliberate override. */
export type ThemePref = 'system' | 'light' | 'dark'

const KEY = 'sansfiltres:theme'
const QUERY = '(prefers-color-scheme: dark)'

/** Whether the operating system is currently asking for a dark interface, kept live. */
function useSystemDark(): boolean {
  const [dark, setDark] = useState(() => {
    try {
      return window.matchMedia(QUERY).matches
    } catch {
      return false
    }
  })

  useEffect(() => {
    let query: MediaQueryList
    try {
      query = window.matchMedia(QUERY)
    } catch {
      return
    }
    const update = () => setDark(query.matches)
    update()
    query.addEventListener('change', update)
    return () => query.removeEventListener('change', update)
  }, [])

  return dark
}

/**
 * Which way round the palette runs, and the switch to change the standing answer. The
 * choice sticks in this browser; left untouched it simply mirrors the operating system,
 * so someone whose phone is already dark never gets a white flash on arrival.
 */
export function useThemePref() {
  const [pref, setPref] = useLocalStorage<ThemePref>(KEY, 'system')
  const systemDark = useSystemDark()

  const dark = pref === 'system' ? systemDark : pref === 'dark'
  const toggle = useCallback(() => setPref(dark ? 'light' : 'dark'), [dark, setPref])

  return { pref, dark, toggle }
}
