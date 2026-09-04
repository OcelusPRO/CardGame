/**
 * Background music. Nothing plays yet — no track ships with the app — but the plumbing
 * is here so adding one is a single line in [TRACKS].
 *
 * A long track is streamed through an `<audio>` element rather than decoded into memory
 * like the effects: a three-minute loop would otherwise cost tens of megabytes of RAM.
 * Volume changes are ramped by hand, because an abrupt cut between two screens is the
 * one thing that makes background music feel bolted on.
 */

/** Phases that get their own atmosphere. Everything else plays whatever came before. */
export type MusicCue = 'lobby' | 'round' | 'finished'

/**
 * Cue to file, served from `frontend/public/music/`. Add an OGG or MP3 that loops
 * cleanly — Incompetech, Kevin MacLeod, Pixabay and the Free Music Archive all have
 * CC0/CC-BY loops made for this — and mention the credit in the README if the licence
 * asks for it.
 */
export const TRACKS: Partial<Record<MusicCue, string>> = {
  // lobby: '/music/lobby.mp3',
  // round: '/music/round.mp3',
  // finished: '/music/finished.mp3',
}

const FADE_STEP_MS = 50

let element: HTMLAudioElement | null = null
let playing: string | null = null
let fade: ReturnType<typeof setInterval> | null = null
let enabled = true
let volume = 0.35

/**
 * Starts [src] on a loop, cross-fading from whatever was playing. Calling it again with
 * the same file is a no-op, so it is safe to run on every render of a screen.
 */
export function playMusic(src: string | undefined, fadeSeconds = 2): void {
  if (!src || !enabled) {
    if (!src) stopMusic(fadeSeconds)
    return
  }
  if (playing === src && element && !element.paused) return

  stopMusic(0)
  try {
    element = new Audio(src)
  } catch {
    element = null
    return
  }
  element.loop = true
  element.volume = 0
  playing = src
  // Autoplay can still be refused; the next screen change tries again, and by then the
  // player has clicked something.
  void element.play().then(() => rampTo(volume, fadeSeconds)).catch(() => undefined)
}

/** Fades the current track out and lets go of it. */
export function stopMusic(fadeSeconds = 1): void {
  const target = element
  if (!target) return
  playing = null
  element = null
  clearFade()
  if (fadeSeconds <= 0) {
    target.pause()
    target.src = ''
    return
  }
  ramp(target, 0, fadeSeconds, () => {
    target.pause()
    target.src = ''
  })
}

/** Follows the app-wide sound switch: off stops the music, on restarts the last track. */
export function setMusicEnabled(on: boolean): void {
  const was = enabled
  enabled = on
  if (!on) {
    stopMusic(0.4)
    return
  }
  if (!was && lastRequested) playMusic(lastRequested, 1)
}

export function setMusicVolume(level: number): void {
  volume = Math.max(0, Math.min(1, level))
  if (element && !fade) element.volume = volume
}

let lastRequested: string | undefined

/** What a screen asks for, remembered so the switch can bring it back. */
export function requestMusic(src: string | undefined, fadeSeconds = 2): void {
  lastRequested = src
  playMusic(src, fadeSeconds)
}

function rampTo(target: number, seconds: number): void {
  if (element) ramp(element, target, seconds)
}

function ramp(target: HTMLAudioElement, to: number, seconds: number, done?: () => void): void {
  clearFade()
  const steps = Math.max(1, Math.round((seconds * 1000) / FADE_STEP_MS))
  const from = target.volume
  let step = 0
  fade = setInterval(() => {
    step += 1
    const value = from + ((to - from) * step) / steps
    target.volume = Math.max(0, Math.min(1, value))
    if (step >= steps) {
      clearFade()
      done?.()
    }
  }, FADE_STEP_MS)
}

function clearFade(): void {
  if (fade) clearInterval(fade)
  fade = null
}
