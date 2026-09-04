import { useCallback, useEffect } from 'react'
import { useLocalStorage } from '../hooks/useLocalStorage'
import { armAudio, playSound, setSoundEnabled, setSoundVolume } from './engine'
import { setMusicEnabled, setMusicVolume } from './music'

export interface SoundPref {
  enabled: boolean
  /** 0 to 1. No slider exposes it yet; the engine already honours it. */
  volume: number
}

const KEY = 'sansfiltres:sound'
const DEFAULTS: SoundPref = { enabled: true, volume: 0.7 }
/** Music sits under the effects: it is a backdrop, not an event. */
const MUSIC_RATIO = 0.5

/**
 * Whether the app may make noise, kept in this browser, plus the switch to change it.
 * Nothing is audible before the first click anyway — browsers see to that — so an
 * on-by-default game never ambushes anybody.
 */
export function useSoundPref() {
  const [pref, setPref] = useLocalStorage<SoundPref>(KEY, DEFAULTS)
  const enabled = pref?.enabled ?? DEFAULTS.enabled
  const volume = pref?.volume ?? DEFAULTS.volume

  useEffect(() => armAudio(), [])

  useEffect(() => {
    setSoundEnabled(enabled)
    setMusicEnabled(enabled)
  }, [enabled])

  useEffect(() => {
    setSoundVolume(volume)
    setMusicVolume(volume * MUSIC_RATIO)
  }, [volume])

  const toggle = useCallback(() => {
    const next = !enabled
    // Applied before the click below, so switching the sound back on is heard at once
    // instead of on the following tap.
    setSoundEnabled(next)
    setMusicEnabled(next)
    if (next) playSound('click')
    setPref({ ...DEFAULTS, ...pref, enabled: next })
  }, [enabled, pref, setPref])

  const setVolume = useCallback(
    (level: number) => setPref({ ...DEFAULTS, ...pref, volume: Math.max(0, Math.min(1, level)) }),
    [pref, setPref],
  )

  return { enabled, volume, toggle, setVolume }
}
