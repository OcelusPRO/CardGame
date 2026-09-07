import type { GameSettingsView } from '../api/types'

/**
 * How long the current step runs, which is the full length the countdown bar measures
 * itself against. The server only sends a deadline; the bar needs the whole to draw a
 * fraction of it.
 */
export function phaseLengthSeconds(phase: string, settings: GameSettingsView): number {
  if (phase === 'SUBMITTING') return settings.submitSeconds
  // A ladder runs one clock per duel, so the bar has to measure a duel.
  if (phase === 'SELECTING') {
    return settings.selectionFormat === 'DUELS' ? settings.duelSeconds : settings.selectSeconds
  }
  return settings.resultSeconds
}
