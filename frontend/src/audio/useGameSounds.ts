import { useEffect, useRef } from 'react'
import type { GamePhase, GameView } from '../api/types'
import { playSound } from './engine'
import { TRACKS, requestMusic, type MusicCue } from './music'

/** How long the reveal takes to land before the winner gets their own sting. */
const WIN_DELAY_MS = 900
/** The last seconds of a phase get a tick, rising in pitch as the deadline closes. */
const TICK_FROM = 5

/**
 * Turns the table into sound: the phases the server announces, and who just won. Kept in
 * one place because these are reactions to the game state, not to anything a player
 * clicked — the panels themselves stay unaware that the app makes noise.
 */
export function useGameSounds(game: GameView | null): void {
  const seen = useRef<{ phase?: GamePhase; players?: number }>({})

  const phase = game?.phase
  const players = game?.players.length
  const youWon = Boolean(
    game?.round?.outcome?.winners.includes(game.you.id) && game.phase === 'ROUND_RESULT',
  )

  useEffect(() => {
    const previous = seen.current.phase
    seen.current.phase = phase
    // First sight of the table, or a reconnection: the state did not *change*, we simply
    // arrived. Announcing it would greet every refresh with a fanfare.
    if (!phase || previous === undefined || previous === phase) return

    if (phase === 'SUBMITTING') playSound('deal')
    if (phase === 'SELECTING') playSound('voteOpen')
    if (phase === 'ROUND_RESULT') playSound('voteEnd')
    if (phase === 'FINISHED') playSound('fanfare')
  }, [phase])

  useEffect(() => {
    if (!youWon) return
    const timer = setTimeout(() => playSound('win'), WIN_DELAY_MS)
    return () => clearTimeout(timer)
  }, [youWon])

  useEffect(() => {
    const previous = seen.current.players
    seen.current.players = players
    if (players === undefined || previous === undefined) return
    if (players > previous && phase === 'LOBBY') playSound('join')
  }, [players, phase])

  // Silent until a track is dropped into `TRACKS`; the wiring is live either way.
  useEffect(() => {
    requestMusic(phase ? TRACKS[cueFor(phase)] : undefined)
  }, [phase])
}

function cueFor(phase: GamePhase): MusicCue {
  if (phase === 'LOBBY') return 'lobby'
  if (phase === 'FINISHED') return 'finished'
  return 'round'
}

/**
 * The countdown heard rather than read. Only a real step down is played, so the quarter
 * second polling of the clock does not turn the last five seconds into a drone.
 */
export function useTimerSound(remaining: number | null, active = true): void {
  const previous = useRef<number | null>(null)

  useEffect(() => {
    const before = previous.current
    previous.current = remaining
    if (!active || remaining === null || before === null || remaining >= before) return

    if (remaining === 0) {
      playSound('timeUp')
      return
    }
    if (remaining <= TICK_FROM) playSound('tick', { rate: 1 + (TICK_FROM - remaining) * 0.09 })
  }, [remaining, active])
}
