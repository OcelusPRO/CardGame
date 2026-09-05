import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { aGame, aPlayer } from '../../test/gameFixtures'
import { PlayerList } from './PlayerList'

function table() {
  const base = aGame()
  return {
    ...base,
    phase: 'LOBBY' as const,
    players: [aPlayer('alice', 'Alice'), aPlayer('bob', 'Bob')],
    you: { ...base.you, id: 'bob', isHost: false },
  }
}

describe('PlayerList — scores', () => {
  it('keeps them out of the salon, where there is nothing to count yet', () => {
    render(<PlayerList game={table()} />)

    expect(screen.queryByText('0')).not.toBeInTheDocument()
  })

  it('shows them once the game is running', () => {
    const base = table()
    render(
      <PlayerList
        game={{
          ...base,
          phase: 'SUBMITTING',
          players: [aPlayer('alice', 'Alice', { score: 7 }), aPlayer('bob', 'Bob', { score: 3 })],
        }}
      />,
    )

    expect(screen.getByText('7')).toBeInTheDocument()
    expect(screen.getByText('3')).toBeInTheDocument()
  })
})

describe('PlayerList — leaving your own seat', () => {
  it('shows the leave button on your row only', () => {
    render(<PlayerList game={table()} onLeave={vi.fn()} />)

    expect(screen.getAllByRole('button', { name: 'Quitter la partie' })).toHaveLength(1)
  })

  it('arms first, then leaves on the second tap', async () => {
    const onLeave = vi.fn()
    const user = userEvent.setup()
    render(<PlayerList game={table()} onLeave={onLeave} />)

    await user.click(screen.getByRole('button', { name: 'Quitter la partie' }))
    expect(onLeave).not.toHaveBeenCalled()

    await user.click(screen.getByRole('button', { name: 'Confirmer et quitter la partie' }))
    expect(onLeave).toHaveBeenCalledOnce()
  })

  it('stays out of the way when no handler is given', () => {
    render(<PlayerList game={table()} />)

    expect(screen.queryByRole('button', { name: /quitter la partie/i })).not.toBeInTheDocument()
  })
})
