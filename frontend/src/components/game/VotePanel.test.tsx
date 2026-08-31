import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { GameView } from '../../api/types'
import { aGame } from '../../test/gameFixtures'
import { VotePanel } from './VotePanel'

function votingGame(overrides: Partial<GameView> = {}): GameView {
  const base = aGame()
  return {
    ...base,
    phase: 'SELECTING',
    you: { ...base.you, mustAnswer: false, mustVote: true },
    round: {
      ...base.round!,
      answers: [
        { id: 0, texts: ['un chat mouillé'], filledText: "Le pire, c'est un chat mouillé.", isMine: true },
        { id: 1, texts: ['la honte'], filledText: "Le pire, c'est la honte.", isMine: false },
      ],
    },
    ...overrides,
  }
}

describe('VotePanel', () => {
  it('sends the vote for the answer that was clicked', async () => {
    const onChoose = vi.fn()
    render(<VotePanel game={votingGame()} onChoose={onChoose} />)

    await userEvent.click(screen.getByRole('button', { name: 'la honte' }))

    expect(onChoose).toHaveBeenCalledWith(1)
  })

  it('never lets a player vote for their own answer', async () => {
    const onChoose = vi.fn()
    render(<VotePanel game={votingGame()} onChoose={onChoose} />)

    await userEvent.click(screen.getByText('un chat mouillé'))

    expect(onChoose).not.toHaveBeenCalled()
  })

  it('writes the pointed answer into the situation card', async () => {
    render(<VotePanel game={votingGame()} onChoose={vi.fn()} />)
    expect(screen.getAllByText('la honte')).toHaveLength(1)

    await userEvent.hover(screen.getByRole('button', { name: 'la honte' }))

    expect(screen.getAllByText('la honte')).toHaveLength(2)
  })

  it('keeps showing the answer it voted for', () => {
    const game = votingGame()
    render(
      <VotePanel game={{ ...game, round: { ...game.round!, myVote: 1 } }} onChoose={vi.fn()} />,
    )

    expect(screen.getAllByText('la honte')).toHaveLength(2)
  })

  it('keeps the authors hidden while the vote is open', () => {
    render(<VotePanel game={votingGame()} onChoose={vi.fn()} />)

    expect(screen.queryByText('Bob')).not.toBeInTheDocument()
    expect(screen.getByText('Votez pour la meilleure réponse.')).toBeInTheDocument()
  })

  it('says the vote is in once it has been cast', () => {
    const game = votingGame()
    render(<VotePanel game={{ ...game, you: { ...game.you, mustVote: false } }} onChoose={vi.fn()} />)

    expect(screen.getByText(/Vote enregistré/i)).toBeInTheDocument()
  })

  it('hands the decision to the czar in czar mode', () => {
    const game = votingGame()
    render(
      <VotePanel
        game={{ ...game, settings: { ...game.settings, selectionMode: 'CZAR' } }}
        onChoose={vi.fn()}
      />,
    )

    expect(screen.getByText('À vous de trancher.')).toBeInTheDocument()
  })
})
