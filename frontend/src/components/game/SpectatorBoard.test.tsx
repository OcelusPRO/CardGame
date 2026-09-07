import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import type { GameView } from '../../api/types'
import { aGame, aCard } from '../../test/gameFixtures'
import { SpectatorBoard } from './SpectatorBoard'

/**
 * What the server actually sends the stream page: an empty `you`, and answers nobody owns
 * until the reveal. The hand is put back in on purpose in one test below, to check the
 * screen would not draw it even if a snapshot arrived carrying one.
 */
function watched(overrides: Partial<GameView> = {}): GameView {
  const base = aGame()
  return {
    ...base,
    spectator: true,
    you: { id: '', hand: [], isHost: false, isCzar: false, mustAnswer: false, mustVote: false },
    ...overrides,
  }
}

describe('SpectatorBoard', () => {
  it('shows the situation and who has played, never a hand', () => {
    const game = watched({
      players: [
        { ...aGame().players[0], hasAnswered: true },
        { ...aGame().players[1], hasAnswered: false },
      ],
      // A hand that should never have been sent, and must not be drawn even so.
      you: { id: '', hand: [aCard('p1', 'un chat mouillé')], isHost: false, isCzar: false, mustAnswer: false, mustVote: false },
    })

    render(<SpectatorBoard game={game} />)

    expect(screen.getByText(/Le pire, c'est/)).toBeInTheDocument()
    expect(screen.getByText('1 / 2')).toBeInTheDocument()
    expect(screen.queryByText('un chat mouillé')).not.toBeInTheDocument()
  })

  it('lays the answers out during the vote, and lets nobody cast one', async () => {
    const base = aGame()
    const game = watched({
      phase: 'SELECTING',
      round: {
        ...base.round!,
        answers: [
          { id: 0, texts: ['un chat mouillé'], filledText: '…', isMine: false },
          { id: 1, texts: ['la honte'], filledText: '…', isMine: false },
        ],
      },
    })

    render(<SpectatorBoard game={game} />)

    expect(screen.getByText('un chat mouillé')).toBeInTheDocument()
    expect(screen.getByText('la honte')).toBeInTheDocument()
    // Nothing on this screen is a button: an inert card is not offered as one.
    expect(screen.queryByRole('button', { name: 'la honte' })).not.toBeInTheDocument()
    await userEvent.click(screen.getByText('la honte'))
    expect(screen.getByText(/La table vote/)).toBeInTheDocument()
  })

  it('never puts the game code on screen, which is what would open a seat', () => {
    render(<SpectatorBoard game={watched({ phase: 'LOBBY' })} />)

    expect(screen.queryByText(/ABCDE/)).not.toBeInTheDocument()
    expect(screen.getByText(/pas encore commencé/)).toBeInTheDocument()
  })
})
