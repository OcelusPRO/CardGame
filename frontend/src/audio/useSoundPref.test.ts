import { act, renderHook } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useSoundPref } from './useSoundPref'

const audio = vi.hoisted(() => ({
  armAudio: vi.fn(() => () => undefined),
  playSound: vi.fn(),
  setSoundEnabled: vi.fn(),
  setSoundVolume: vi.fn(),
  setMusicEnabled: vi.fn(),
  setMusicVolume: vi.fn(),
}))

vi.mock('./engine', () => ({
  armAudio: audio.armAudio,
  playSound: audio.playSound,
  setSoundEnabled: audio.setSoundEnabled,
  setSoundVolume: audio.setSoundVolume,
}))
vi.mock('./music', () => ({
  setMusicEnabled: audio.setMusicEnabled,
  setMusicVolume: audio.setMusicVolume,
}))

function stored() {
  return JSON.parse(window.localStorage.getItem('sansfiltres:sound') ?? 'null')
}

describe('useSoundPref', () => {
  beforeEach(() => {
    window.localStorage.clear()
    Object.values(audio).forEach((spy) => spy.mockClear())
  })

  it('starts audible, because a party game that says nothing is odd', () => {
    const { result } = renderHook(() => useSoundPref())
    expect(result.current.enabled).toBe(true)
    expect(audio.setSoundEnabled).toHaveBeenCalledWith(true)
  })

  it('remembers a mute across visits', () => {
    const first = renderHook(() => useSoundPref())
    act(() => first.result.current.toggle())

    expect(first.result.current.enabled).toBe(false)
    expect(stored()).toMatchObject({ enabled: false })

    const second = renderHook(() => useSoundPref())
    expect(second.result.current.enabled).toBe(false)
  })

  it('cuts the music with the effects', () => {
    const { result } = renderHook(() => useSoundPref())
    act(() => result.current.toggle())
    expect(audio.setMusicEnabled).toHaveBeenCalledWith(false)
  })

  it('answers the tap that switches the sound back on', () => {
    const { result } = renderHook(() => useSoundPref())
    act(() => result.current.toggle())
    audio.playSound.mockClear()

    act(() => result.current.toggle())
    expect(audio.playSound).toHaveBeenCalledWith('click')
  })

  it('keeps the music under the effects', () => {
    const { result } = renderHook(() => useSoundPref())
    act(() => result.current.setVolume(0.8))
    expect(audio.setSoundVolume).toHaveBeenLastCalledWith(0.8)
    expect(audio.setMusicVolume).toHaveBeenLastCalledWith(0.4)
  })
})
