import { SAMPLES, SOUNDS, type SoundName, type Voice } from './sounds'

/**
 * The one audio context of the app, and the only thing that ever makes noise.
 *
 * Everything is lazy and guarded. A browser without Web Audio, a test running in jsdom,
 * a tab where the user has never clicked: in all three cases every call here quietly does
 * nothing rather than throwing into the middle of a render.
 */

interface Options {
  /** Pitch multiplier — 1.06 is a semitone up. Also speeds up a sample. */
  rate?: number
  /** Level multiplier on top of the sound's own gain. */
  gain?: number
}

const DEFAULT_ATTACK = 0.004
/** Web Audio ramps are exponential, and an exponential curve can never reach zero. */
const SILENCE = 0.0001

let context: AudioContext | null = null
let sfxBus: GainNode | null = null
let noise: AudioBuffer | null = null
let enabled = true
let volume = 0.7

const samples = new Map<SoundName, AudioBuffer>()
const loading = new Set<SoundName>()
const lastPlayed = new Map<SoundName, number>()

function createContext(): AudioContext | null {
  if (context) return context
  const Ctor =
    typeof window === 'undefined'
      ? undefined
      : window.AudioContext ?? (window as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext
  if (!Ctor) return null
  try {
    context = new Ctor()
    sfxBus = context.createGain()
    sfxBus.gain.value = volume
    sfxBus.connect(context.destination)
  } catch {
    context = null
    sfxBus = null
  }
  return context
}

/**
 * Wakes the audio up on the first gesture of the page. Browsers refuse to start a context
 * before the user has touched something, and a context created too early stays suspended
 * for good on some of them — so we build it here, from inside the gesture.
 */
export function armAudio(): () => void {
  if (typeof window === 'undefined') return () => undefined
  const events = ['pointerdown', 'keydown', 'touchstart'] as const

  const wake = () => {
    const ctx = createContext()
    void ctx?.resume().catch(() => undefined)
    if (ctx) preloadSamples()
    if (!ctx || ctx.state === 'running') release()
  }
  const release = () => events.forEach((event) => window.removeEventListener(event, wake))

  events.forEach((event) => window.addEventListener(event, wake, { passive: true }))
  return release
}

/** Whether effects and music may be heard at all. */
export function setSoundEnabled(on: boolean): void {
  enabled = on
}

/** Master level for the effects bus, 0 to 1. */
export function setSoundVolume(level: number): void {
  volume = Math.max(0, Math.min(1, level))
  if (sfxBus && context) sfxBus.gain.setTargetAtTime(volume, context.currentTime, 0.01)
}

export function isSoundEnabled(): boolean {
  return enabled
}

/** Plays one effect. Safe to call from anywhere, including a render that never mounts. */
export function playSound(name: SoundName, options: Options = {}): void {
  if (!enabled) return
  const sound = SOUNDS[name]
  if (!sound) return

  const now = Date.now()
  if (sound.throttleMs && now - (lastPlayed.get(name) ?? 0) < sound.throttleMs) return

  const ctx = createContext()
  if (!ctx || !sfxBus || ctx.state === 'closed') return
  // A context suspended by the autoplay policy would swallow the sound anyway; asking it
  // to resume from inside the click that triggered this is exactly what unblocks it.
  if (ctx.state === 'suspended') void ctx.resume().catch(() => undefined)

  lastPlayed.set(name, now)

  const sample = samples.get(name)
  if (sample) {
    playBuffer(ctx, sfxBus, sample, options)
    return
  }
  if (SAMPLES[name] && !loading.has(name)) void loadSample(name)

  const at = ctx.currentTime + 0.001
  sound.voices.forEach((voice) => renderVoice(ctx, sfxBus as GainNode, voice, at, options))
}

/** The music bus lives in `music.ts`; it borrows the context so both share one clock. */
export function audioContext(): AudioContext | null {
  return createContext()
}

function playBuffer(ctx: AudioContext, out: AudioNode, buffer: AudioBuffer, options: Options): void {
  const source = ctx.createBufferSource()
  source.buffer = buffer
  source.playbackRate.value = options.rate ?? 1
  const gain = ctx.createGain()
  gain.gain.value = options.gain ?? 1
  source.connect(gain).connect(out)
  source.start()
  source.onended = () => {
    source.disconnect()
    gain.disconnect()
  }
}

function renderVoice(ctx: AudioContext, out: AudioNode, voice: Voice, at: number, options: Options): void {
  const rate = options.rate ?? 1
  const start = at + (voice.at ?? 0)
  const end = start + voice.dur
  const attack = Math.min(voice.attack ?? DEFAULT_ATTACK, voice.dur / 2)
  const peak = Math.max(SILENCE * 2, voice.gain * (options.gain ?? 1))

  const envelope = ctx.createGain()
  envelope.gain.setValueAtTime(SILENCE, start)
  envelope.gain.exponentialRampToValueAtTime(peak, start + attack)
  envelope.gain.exponentialRampToValueAtTime(SILENCE, end)
  envelope.connect(out)

  const from = voice.freq * rate
  const to = (voice.slide ?? voice.freq) * rate
  let source: AudioScheduledSourceNode

  if (voice.wave === 'noise') {
    const buffer = noiseBuffer(ctx)
    const player = ctx.createBufferSource()
    player.buffer = buffer
    player.loop = true
    const band = ctx.createBiquadFilter()
    band.type = 'bandpass'
    band.Q.value = voice.q ?? 1
    band.frequency.setValueAtTime(from, start)
    if (to !== from) band.frequency.exponentialRampToValueAtTime(to, end)
    player.connect(band).connect(envelope)
    source = player
  } else {
    const oscillator = ctx.createOscillator()
    oscillator.type = voice.wave
    oscillator.frequency.setValueAtTime(from, start)
    if (to !== from) oscillator.frequency.exponentialRampToValueAtTime(to, end)
    oscillator.connect(envelope)
    source = oscillator
  }

  source.start(start)
  source.stop(end + 0.02)
  source.onended = () => {
    source.disconnect()
    envelope.disconnect()
  }
}

/** One second of white noise, reused by every breathy voice in the catalogue. */
function noiseBuffer(ctx: AudioContext): AudioBuffer {
  if (noise) return noise
  const buffer = ctx.createBuffer(1, ctx.sampleRate, ctx.sampleRate)
  const data = buffer.getChannelData(0)
  for (let i = 0; i < data.length; i += 1) data[i] = Math.random() * 2 - 1
  noise = buffer
  return buffer
}

async function loadSample(name: SoundName): Promise<void> {
  const url = SAMPLES[name]
  const ctx = createContext()
  if (!url || !ctx) return
  loading.add(name)
  try {
    const response = await fetch(url)
    if (!response.ok) return
    samples.set(name, await ctx.decodeAudioData(await response.arrayBuffer()))
  } catch {
    // The synthesised voice stays in charge. A missing file is not worth a broken screen.
  } finally {
    loading.delete(name)
  }
}

/** Warms up whatever recordings are configured, so the first play is not the slow one. */
export function preloadSamples(): void {
  ;(Object.keys(SAMPLES) as SoundName[]).forEach((name) => {
    if (!samples.has(name) && !loading.has(name)) void loadSample(name)
  })
}

/** Test seam: drops the context and every cached buffer. */
export function resetAudioForTests(): void {
  void context?.close().catch(() => undefined)
  context = null
  sfxBus = null
  noise = null
  samples.clear()
  loading.clear()
  lastPlayed.clear()
  enabled = true
  volume = 0.7
}
