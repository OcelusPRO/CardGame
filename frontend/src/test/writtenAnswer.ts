import { screen } from '@testing-library/react'

/**
 * Finds an answer as it appears written into a situation card.
 *
 * The card writes it word by word — each word its own box, so the sentence can wrap —
 * which means no single element holds the answer as one run of text, and the plain
 * `getByText` cannot see it. This matches the span that wraps the words instead, and
 * rejects any ancestor that merely contains it.
 */
export function writtenIntoSituation(answer: string): HTMLElement[] {
  return screen.queryAllByText((_, element) => {
    if (!element || element.tagName !== 'SPAN') return false
    if (normalize(element.textContent) !== answer) return false
    // The wrapper holds one word per child; an ancestor holding the whole answer in a
    // single child is a container, not the written answer itself.
    return !Array.from(element.children).some((child) => normalize(child.textContent) === answer)
  })
}

function normalize(text: string | null): string {
  return (text ?? '').replace(/\s+/g, ' ').trim()
}
