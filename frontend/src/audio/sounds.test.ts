import { describe, expect, it } from 'vitest'
import { SAMPLES, SOUNDS, type SoundName } from './sounds'

describe('the sound catalogue', () => {
  const names = Object.keys(SOUNDS) as SoundName[]

  it('gives every effect something to play', () => {
    names.forEach((name) => expect(SOUNDS[name].voices.length).toBeGreaterThan(0))
  })

  it('keeps every voice audible and finite', () => {
    names.forEach((name) =>
      SOUNDS[name].voices.forEach((voice) => {
        expect(voice.dur).toBeGreaterThan(0)
        expect(voice.gain).toBeGreaterThan(0)
        // Exponential ramps cannot pass through zero, so a pitch of nought is silence.
        expect(voice.freq).toBeGreaterThan(0)
        if (voice.slide !== undefined) expect(voice.slide).toBeGreaterThan(0)
      }),
    )
  })

  it('stays inside the master headroom when a whole sound fires at once', () => {
    names.forEach((name) => {
      const loudest = SOUNDS[name].voices
        .filter((voice) => (voice.at ?? 0) === 0)
        .reduce((total, voice) => total + voice.gain, 0)
      expect(loudest).toBeLessThanOrEqual(1)
    })
  })

  it('only overrides effects that exist', () => {
    Object.keys(SAMPLES).forEach((name) => expect(names).toContain(name))
  })
})
