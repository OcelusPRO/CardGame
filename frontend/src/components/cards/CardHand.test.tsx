import { render, screen } from '@testing-library/react'
import { MotionConfig } from 'motion/react'
import { describe, expect, it, vi } from 'vitest'
import { CardHand } from './CardHand'
import type { PunchlineCardView } from '../../api/types'

const hand: PunchlineCardView[] = [
  { id: 'a', text: 'un chat mouillé', custom: false, blankCount: 0 },
  { id: 'b', text: 'la honte de ma vie', custom: false, blankCount: 0 },
]

describe('CardHand', () => {
  it('shows every card face up, ready to pick', () => {
    render(
      <MotionConfig reducedMotion="always">
        <CardHand cards={hand} selected={[]} onToggle={vi.fn()} />
      </MotionConfig>,
    )
    expect(screen.getByRole('button', { name: 'un chat mouillé' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'la honte de ma vie' })).toBeInTheDocument()
  })

  it('skips the deal — no card back — when motion is switched off', () => {
    const { container } = render(
      <MotionConfig reducedMotion="always">
        <CardHand cards={hand} selected={[]} onToggle={vi.fn()} />
      </MotionConfig>,
    )
    // The deck back is the only thing in the hand that carries an <img>.
    expect(container.querySelector('img')).toBeNull()
  })

  it('deals from the deck back when motion is allowed', () => {
    const { container } = render(
      <MotionConfig reducedMotion="never">
        <CardHand cards={hand} selected={[]} onToggle={vi.fn()} />
      </MotionConfig>,
    )
    expect(container.querySelector('img')).not.toBeNull()
    // Faces are still in the tree underneath, so a card is pickable straight away.
    expect(screen.getByRole('button', { name: 'un chat mouillé' })).toBeInTheDocument()
  })
})
