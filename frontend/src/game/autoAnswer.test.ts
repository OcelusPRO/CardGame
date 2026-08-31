import { describe, expect, it } from 'vitest'
import type { PunchlineCardView } from '../api/types'
import { pickAutoAnswer, shouldAutoSubmit } from './autoAnswer'

const hand: PunchlineCardView[] = ['p1', 'p2', 'p3'].map((id) => ({
  id,
  text: id,
  custom: false,
  blankCount: 0,
}))

describe('pickAutoAnswer', () => {
  it('sends the selection untouched when it is already complete', () => {
    expect(pickAutoAnswer(['p2'], hand, 1)).toEqual(['p2'])
  })

  it('draws a card at random when nothing was selected', () => {
    expect(pickAutoAnswer([], hand, 1, () => 0)).toEqual(['p1'])
  })

  it('completes a partial selection without repeating a card', () => {
    const picked = pickAutoAnswer(['p2'], hand, 2, () => 0)

    expect(picked[0]).toBe('p2')
    expect(new Set(picked).size).toBe(2)
  })

  it('ignores a card that is no longer in hand', () => {
    expect(pickAutoAnswer(['gone'], hand, 1, () => 0)).toEqual(['p1'])
  })

  it('never invents a card when the hand is empty', () => {
    expect(pickAutoAnswer([], [], 1)).toEqual([])
  })
})

describe('shouldAutoSubmit', () => {
  it('waits while there is still time', () => {
    expect(shouldAutoSubmit(20)).toBe(false)
  })

  it('fires just before the deadline', () => {
    expect(shouldAutoSubmit(1)).toBe(true)
  })

  it('does nothing when there is no timer at all', () => {
    expect(shouldAutoSubmit(null)).toBe(false)
  })
})
