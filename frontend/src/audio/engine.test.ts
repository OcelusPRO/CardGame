import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { playSound, resetAudioForTests, setSoundEnabled, setSoundVolume } from './engine'

/** jsdom has no Web Audio at all, so the engine is measured against a stand-in. */
function param(value = 0) {
  return {
    value,
    setValueAtTime: () => undefined,
    exponentialRampToValueAtTime: () => undefined,
    setTargetAtTime: () => undefined,
  }
}

function node<T extends object>(extra: T) {
  return {
    connect(next: unknown) {
      return next
    },
    disconnect: () => undefined,
    ...extra,
  }
}

class FakeAudioContext {
  static built: FakeAudioContext[] = []
  state: AudioContextState = 'running'
  currentTime = 0
  sampleRate = 48000
  destination = node({})
  oscillators: { type: string; started: number[] }[] = []
  buffers = 0
  resumed = 0

  constructor() {
    FakeAudioContext.built.push(this)
  }

  createGain() {
    return node({ gain: param(1) })
  }

  createBiquadFilter() {
    return node({ type: 'bandpass', Q: param(1), frequency: param(440) })
  }

  createOscillator() {
    const record = { type: 'sine', started: [] as number[] }
    this.oscillators.push(record)
    return node({
      get type() {
        return record.type
      },
      set type(value: string) {
        record.type = value
      },
      frequency: param(440),
      start: (at: number) => record.started.push(at),
      stop: () => undefined,
      onended: null,
    })
  }

  createBufferSource() {
    this.buffers += 1
    return node({
      buffer: null,
      loop: false,
      playbackRate: param(1),
      start: () => undefined,
      stop: () => undefined,
      onended: null,
    })
  }

  createBuffer(_channels: number, length: number) {
    return { getChannelData: () => new Float32Array(length) }
  }

  resume() {
    this.resumed += 1
    return Promise.resolve()
  }

  close() {
    return Promise.resolve()
  }
}

function context(): FakeAudioContext {
  return FakeAudioContext.built[FakeAudioContext.built.length - 1]
}

describe('the audio engine', () => {
  const real = window.AudioContext

  beforeEach(() => {
    FakeAudioContext.built = []
    window.AudioContext = FakeAudioContext as unknown as typeof AudioContext
    resetAudioForTests()
  })

  afterEach(() => {
    window.AudioContext = real
    resetAudioForTests()
  })

  it('renders one source per voice of the sound', () => {
    playSound('vote')
    // Two sine notes and one burst of noise.
    expect(context().oscillators).toHaveLength(2)
    expect(context().buffers).toBe(1)
  })

  it('opens exactly one context however many sounds are played', () => {
    playSound('click')
    playSound('voteEnd')
    expect(FakeAudioContext.built).toHaveLength(1)
  })

  it('stays silent while the sound is switched off', () => {
    setSoundEnabled(false)
    playSound('click')
    expect(FakeAudioContext.built).toHaveLength(0)

    setSoundEnabled(true)
    playSound('click')
    expect(context().oscillators.length).toBeGreaterThan(0)
  })

  it('refuses to machine-gun a throttled sound', () => {
    playSound('cardSelect')
    const first = context().oscillators.length
    playSound('cardSelect')
    expect(context().oscillators).toHaveLength(first)
  })

  it('lets an untriggered sound through even so', () => {
    playSound('cardSelect')
    playSound('cardDeselect')
    expect(context().oscillators.length).toBeGreaterThan(1)
  })

  it('does nothing at all on a browser without Web Audio', () => {
    resetAudioForTests()
    // @ts-expect-error the point of the test is the missing constructor
    delete window.AudioContext
    expect(() => playSound('click')).not.toThrow()
    expect(() => setSoundVolume(0.2)).not.toThrow()
  })
})
