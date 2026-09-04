import { act, renderHook } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { GameView } from '../api/types'
import { aGame, aPlayer } from '../test/gameFixtures'
import { useGameSounds, useTimerSound } from './useGameSounds'

const { playSound, requestMusic } = vi.hoisted(() => ({
  playSound: vi.fn(),
  requestMusic: vi.fn(),
}))

vi.mock('./engine', () => ({ playSound }))
vi.mock('./music', () => ({ TRACKS: {}, requestMusic }))

function played(): string[] {
  return playSound.mock.calls.map(([name]) => name as string)
}

describe('useGameSounds', () => {
  beforeEach(() => {
    playSound.mockClear()
    requestMusic.mockClear()
  })

  it('says nothing when it merely discovers a table in progress', () => {
    renderHook(() => useGameSounds(aGame({ phase: 'SELECTING' })))
    expect(played()).toEqual([])
  })

  it('announces each step the server moves to', () => {
    const { rerender } = renderHook(({ game }: { game: GameView }) => useGameSounds(game), {
      initialProps: { game: aGame({ phase: 'LOBBY' }) },
    })

    rerender({ game: aGame({ phase: 'SUBMITTING' }) })
    rerender({ game: aGame({ phase: 'SELECTING' }) })
    rerender({ game: aGame({ phase: 'ROUND_RESULT' }) })
    rerender({ game: aGame({ phase: 'FINISHED' }) })

    expect(played()).toEqual(['deal', 'voteOpen', 'voteEnd', 'fanfare'])
  })

  it('does not repeat itself while a phase merely refreshes', () => {
    const { rerender } = renderHook(({ game }: { game: GameView }) => useGameSounds(game), {
      initialProps: { game: aGame({ phase: 'LOBBY' }) },
    })

    rerender({ game: aGame({ phase: 'SELECTING' }) })
    rerender({ game: aGame({ phase: 'SELECTING' }) })

    expect(played()).toEqual(['voteOpen'])
  })

  it('congratulates the winner once the reveal has landed', () => {
    vi.useFakeTimers()
    const base = aGame()
    const result = aGame({
      phase: 'ROUND_RESULT',
      round: { ...base.round!, outcome: { points: { alice: 2 }, winners: ['alice'] } },
    })

    const { rerender } = renderHook(({ game }: { game: GameView }) => useGameSounds(game), {
      initialProps: { game: aGame({ phase: 'SELECTING' }) },
    })
    rerender({ game: result })
    act(() => vi.advanceTimersByTime(1000))

    expect(played()).toEqual(['voteEnd', 'win'])
    vi.useRealTimers()
  })

  it('leaves the loser to their silence', () => {
    vi.useFakeTimers()
    const base = aGame()
    const result = aGame({
      phase: 'ROUND_RESULT',
      round: { ...base.round!, outcome: { points: { bob: 2 }, winners: ['bob'] } },
    })

    const { rerender } = renderHook(({ game }: { game: GameView }) => useGameSounds(game), {
      initialProps: { game: aGame({ phase: 'SELECTING' }) },
    })
    rerender({ game: result })
    act(() => vi.advanceTimersByTime(1000))

    expect(played()).toEqual(['voteEnd'])
    vi.useRealTimers()
  })

  it('greets a player taking a seat, but only in the lobby', () => {
    const alone = aGame({ phase: 'LOBBY', players: [aPlayer('alice', 'Alice')] })
    const { rerender } = renderHook(({ game }: { game: GameView }) => useGameSounds(game), {
      initialProps: { game: alone },
    })

    rerender({
      game: aGame({ phase: 'LOBBY', players: [aPlayer('alice', 'Alice'), aPlayer('bob', 'Bob')] }),
    })

    expect(played()).toEqual(['join'])
  })

  it('asks for the track matching the step, even when there is none yet', () => {
    renderHook(() => useGameSounds(aGame({ phase: 'LOBBY' })))
    expect(requestMusic).toHaveBeenCalledWith(undefined)
  })
})

describe('useTimerSound', () => {
  beforeEach(() => playSound.mockClear())

  it('holds its tongue until the last seconds', () => {
    const { rerender } = renderHook(({ left }: { left: number | null }) => useTimerSound(left), {
      initialProps: { left: 20 as number | null },
    })

    rerender({ left: 19 })
    rerender({ left: 6 })
    expect(played()).toEqual([])

    rerender({ left: 5 })
    expect(played()).toEqual(['tick'])
  })

  it('climbs in pitch as the deadline closes', () => {
    const { rerender } = renderHook(({ left }: { left: number | null }) => useTimerSound(left), {
      initialProps: { left: 3 as number | null },
    })

    rerender({ left: 2 })
    rerender({ left: 1 })

    const rates = playSound.mock.calls.map(([, options]) => (options as { rate: number }).rate)
    expect(rates[1]).toBeGreaterThan(rates[0])
  })

  it('buzzes when time is up, instead of ticking', () => {
    const { rerender } = renderHook(({ left }: { left: number | null }) => useTimerSound(left), {
      initialProps: { left: 1 as number | null },
    })

    rerender({ left: 0 })
    expect(played()).toEqual(['timeUp'])
  })

  it('keeps quiet on a screen that does not chime', () => {
    const { rerender } = renderHook(
      ({ left }: { left: number | null }) => useTimerSound(left, false),
      { initialProps: { left: 3 as number | null } },
    )

    rerender({ left: 2 })
    rerender({ left: 0 })
    expect(played()).toEqual([])
  })
})
