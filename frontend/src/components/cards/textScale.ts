/**
 * Long French words in a narrow card used to be chopped in half. Rather than guessing a
 * size from a breakpoint, the type is sized against the width of the card itself: the
 * shell declares a container, and these values are expressed in `cqi`, one percent of it.
 * A card in a cramped hand and the same card on a wide board both stay readable.
 */

export function punchlineFontSize(text: string): string {
  const length = text.trim().length
  if (length <= 25) return 'clamp(1rem, 10cqi, 1.9rem)'
  if (length <= 55) return 'clamp(0.95rem, 8cqi, 1.55rem)'
  if (length <= 100) return 'clamp(0.9rem, 6.6cqi, 1.25rem)'
  return 'clamp(0.85rem, 5.6cqi, 1.05rem)'
}

export function situationFontSize(text: string): string {
  const length = text.trim().length
  if (length <= 60) return 'clamp(1.4rem, 9.5cqi, 3rem)'
  if (length <= 110) return 'clamp(1.25rem, 7.5cqi, 2.4rem)'
  if (length <= 170) return 'clamp(1.1rem, 6cqi, 1.9rem)'
  return 'clamp(1rem, 5cqi, 1.6rem)'
}

/** The answer written into a decided situation is the loudest thing on the screen. */
export function celebratedAnswerFontSize(text: string): string {
  const length = text.trim().length
  if (length <= 30) return 'clamp(1.8rem, 14cqi, 4rem)'
  if (length <= 70) return 'clamp(1.5rem, 11cqi, 3rem)'
  return 'clamp(1.2rem, 8.5cqi, 2.2rem)'
}

/**
 * Wrapping rules shared by every card: move a word that does not fit down to the next
 * line, whole. Automatic hyphenation was tried and dropped — a card is read at a glance,
 * and "an-glais" split across two lines is a stumble every time. `break-words` stays as
 * the last resort, for the single word too long to fit a line at all.
 */
export const WRAP_CLASSES = 'break-words text-pretty'
