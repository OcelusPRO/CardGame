import type { PunchlineCardView } from '../api/types'

/**
 * What to play when the timer runs out: whatever the player had already picked, completed
 * with random cards from their hand. Nobody sits out a round because they hesitated.
 */
export function pickAutoAnswer(
  selected: string[],
  hand: PunchlineCardView[],
  expected: number,
  random: () => number = Math.random,
): string[] {
  const chosen = selected.filter((id) => hand.some((card) => card.id === id)).slice(0, expected)
  const pool = hand.filter((card) => !chosen.includes(card.id))

  while (chosen.length < expected && pool.length > 0) {
    const index = Math.floor(random() * pool.length)
    chosen.push(pool.splice(index, 1)[0].id)
  }
  return chosen
}

/** True once the deadline is close enough that the answer must leave now. */
export function shouldAutoSubmit(remainingSeconds: number | null): boolean {
  return remainingSeconds !== null && remainingSeconds <= AUTO_SUBMIT_SECONDS
}

/** Sent slightly early, so the answer reaches the server before it closes the step. */
export const AUTO_SUBMIT_SECONDS = 2
