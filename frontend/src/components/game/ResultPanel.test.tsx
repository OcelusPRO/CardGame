import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import type { GameView } from '../../api/types'
import { aGame } from '../../test/gameFixtures'
import { writtenIntoSituation } from '../../test/writtenAnswer'
import { ResultPanel } from './ResultPanel'

function scoredGame(overrides: Partial<GameView> = {}): GameView {
  const base = aGame()
  return {
    ...base,
    phase: 'ROUND_RESULT',
    you: { ...base.you, mustAnswer: false },
    round: {
      ...base.round!,
      answers: [
        {
          id: 0,
          texts: ['un chat mouillé'],
          filledText: "Le pire, c'est un chat mouillé.",
          isMine: true,
          authorId: 'alice',
          votes: 0,
        },
        {
          id: 1,
          texts: ['la honte'],
          filledText: "Le pire, c'est la honte.",
          isMine: false,
          authorId: 'bob',
          votes: 3,
        },
      ],
      outcome: { points: { bob: 4 }, winners: ['bob'], topAnswerId: 1 },
    },
    ...overrides,
  }
}

describe('ResultPanel', () => {
  it('writes the winning answer into the situation card', () => {
    render(<ResultPanel game={scoredGame()} onNext={vi.fn()} />)

    // Once on the answer card, once written large into the situation.
    expect(screen.getByText('la honte')).toBeInTheDocument()
    expect(writtenIntoSituation('la honte')).toHaveLength(1)
  })

  it('names the winner under the situation', () => {
    render(<ResultPanel game={scoredGame()} onNext={vi.fn()} />)

    expect(screen.getByText('Gagnant : Bob')).toBeInTheDocument()
  })

  it('names every winner when the vote was split', () => {
    const game = scoredGame()
    const outcome = { points: { alice: 2, bob: 2 }, winners: ['alice', 'bob'] }
    render(
      <ResultPanel game={{ ...game, round: { ...game.round!, outcome } }} onNext={vi.fn()} />,
    )

    expect(screen.getByText('Ex æquo : Alice, Bob')).toBeInTheDocument()
  })

  it('reveals who wrote what', () => {
    render(<ResultPanel game={scoredGame()} onNext={vi.fn()} />)

    expect(screen.getByText('Bob')).toBeInTheDocument()
    expect(screen.getByText('Alice')).toBeInTheDocument()
  })

  it('shows the points handed out', () => {
    render(<ResultPanel game={scoredGame()} onNext={vi.fn()} />)

    expect(screen.getByText('Bob +4')).toBeInTheDocument()
  })

  it('says so when nobody scored', () => {
    const game = scoredGame()
    const outcome = { points: {}, winners: [] }
    render(<ResultPanel game={{ ...game, round: { ...game.round!, outcome } }} onNext={vi.fn()} />)

    expect(screen.getByText(/Personne n'a marqué/)).toBeInTheDocument()
  })
})
