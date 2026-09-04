import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { playMusic, setMusicEnabled, setMusicVolume, stopMusic } from './music'

/** jsdom builds an `<audio>` element but refuses to play it, so we bring our own. */
class FakeAudio {
  static made: FakeAudio[] = []
  loop = false
  volume = 1
  paused = true
  constructor(public src: string) {
    FakeAudio.made.push(this)
  }
  play() {
    this.paused = false
    return Promise.resolve()
  }
  pause() {
    this.paused = true
  }
}

const last = () => FakeAudio.made[FakeAudio.made.length - 1]

describe('background music', () => {
  const real = window.Audio

  beforeEach(() => {
    vi.useFakeTimers()
    FakeAudio.made = []
    window.Audio = FakeAudio as unknown as typeof Audio
    setMusicEnabled(true)
    setMusicVolume(0.4)
  })

  afterEach(() => {
    stopMusic(0)
    window.Audio = real
    vi.useRealTimers()
  })

  it('does nothing at all while no track is configured', () => {
    playMusic(undefined)
    expect(FakeAudio.made).toHaveLength(0)
  })

  it('fades a track in on a loop', async () => {
    playMusic('/music/lobby.mp3', 1)
    await vi.advanceTimersByTimeAsync(0)

    expect(last().src).toBe('/music/lobby.mp3')
    expect(last().loop).toBe(true)
    expect(last().volume).toBe(0)

    await vi.advanceTimersByTimeAsync(1000)
    expect(last().volume).toBeCloseTo(0.4, 5)
  })

  it('ignores a request for the track already playing', async () => {
    playMusic('/music/lobby.mp3', 1)
    await vi.advanceTimersByTimeAsync(10)
    playMusic('/music/lobby.mp3', 1)

    expect(FakeAudio.made).toHaveLength(1)
  })

  it('swaps tracks when the screen changes', async () => {
    playMusic('/music/lobby.mp3', 1)
    await vi.advanceTimersByTimeAsync(10)
    playMusic('/music/round.mp3', 1)
    await vi.advanceTimersByTimeAsync(10)

    expect(FakeAudio.made).toHaveLength(2)
    expect(FakeAudio.made[0].paused).toBe(true)
    expect(last().src).toBe('/music/round.mp3')
  })

  it('goes quiet with the master switch', async () => {
    playMusic('/music/lobby.mp3', 1)
    await vi.advanceTimersByTimeAsync(1000)

    setMusicEnabled(false)
    await vi.advanceTimersByTimeAsync(1000)
    expect(last().paused).toBe(true)
  })

  it('refuses to start while muted', () => {
    setMusicEnabled(false)
    playMusic('/music/lobby.mp3', 1)
    expect(FakeAudio.made).toHaveLength(0)
  })
})
