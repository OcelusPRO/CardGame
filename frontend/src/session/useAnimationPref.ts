import { useCallback, useEffect, useState } from 'react'
import { useLocalStorage } from '../hooks/useLocalStorage'

/** `system` follows the OS "reduce motion" switch; the others are a deliberate override. */
export type AnimationPref = 'system' | 'on' | 'off'

const KEY = 'sansfiltres:animations'
const QUERY = '(prefers-reduced-motion: reduce)'

/** The OS-level "I want less motion" setting, kept live. */
function useSystemReducedMotion(): boolean {
  const [reduced, setReduced] = useState(() => {
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
    const update = () => setReduced(query.matches)
    update()
    query.addEventListener('change', update)
    return () => query.removeEventListener('change', update)
  }, [])

  return reduced
}

/**
 * Whether the interface may animate, and a switch to change the standing answer. The
 * choice sticks in this browser; left untouched it simply mirrors the operating system.
 */
export function useAnimationPref() {
  const [pref, setPref] = useLocalStorage<AnimationPref>(KEY, 'system')
  const systemReduced = useSystemReducedMotion()

  const enabled = pref === 'system' ? !systemReduced : pref === 'on'
  const toggle = useCallback(() => setPref(enabled ? 'off' : 'on'), [enabled, setPref])

  return { pref, enabled, toggle }
}
