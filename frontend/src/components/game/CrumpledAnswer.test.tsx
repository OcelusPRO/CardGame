import { act, render, screen } from '@testing-library/react'
import { MotionConfig } from 'motion/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

// jsdom has neither WebGL nor a 2D context, so the two halves of the effect — painting
// the face and handing it to the shared stage — stand in for the real thing.
const crumpleCard = vi.fn<(request: unknown) => Promise<void>>(() => Promise.resolve())
vi.mock('./crumple/crumpleStage', () => ({ crumpleCard: (request: unknown) => crumpleCard(request) }))
vi.mock('./crumple/cardTexture', () => ({ drawCardFace: () => document.createElement('canvas') }))

import { CrumpledAnswer } from './CrumpledAnswer'

const face = { text: 'la honte', author: 'Bob', votes: 0 }

/** The operating system asking for less motion — what the header switch has to override. */
function systemAsksForCalm(): void {
  window.matchMedia = ((query: string) => ({
    matches: true,
    media: query,
    onchange: null,
    addEventListener: () => undefined,
    removeEventListener: () => undefined,
    addListener: () => undefined,
    removeListener: () => undefined,
    dispatchEvent: () => false,
  })) as unknown as typeof window.matchMedia
}

let rect: ReturnType<Element['getBoundingClientRect']>

beforeEach(() => {
  vi.useFakeTimers()
  crumpleCard.mockClear()
  systemAsksForCalm()
  rect = { left: 10, top: 20, width: 200, height: 300 } as DOMRect
  vi.spyOn(Element.prototype, 'getBoundingClientRect').mockReturnValue(rect)
})

afterEach(() => {
  vi.useRealTimers()
  vi.restoreAllMocks()
})

describe('CrumpledAnswer', () => {
  it('still crumples for a player whose system asks for calm but who turned animations on', async () => {
    render(
      <MotionConfig reducedMotion="never">
        <CrumpledAnswer face={face} delay={0.5}>
          <p>la honte</p>
        </CrumpledAnswer>
      </MotionConfig>,
    )

    await act(async () => {
      vi.advanceTimersByTime(600)
    })

    expect(crumpleCard).toHaveBeenCalledTimes(1)
  })

  it('leaves the card on the table when animations are switched off', async () => {
    render(
      <MotionConfig reducedMotion="always">
        <CrumpledAnswer face={face} delay={0.5}>
          <p>la honte</p>
        </CrumpledAnswer>
      </MotionConfig>,
    )

    await act(async () => {
      vi.advanceTimersByTime(600)
    })

    expect(crumpleCard).not.toHaveBeenCalled()
    expect(screen.getByText('la honte')).toBeInTheDocument()
  })
})
