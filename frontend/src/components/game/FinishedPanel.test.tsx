import { render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import type { GameView } from '../../api/types'
import { aGame, aPlayer } from '../../test/gameFixtures'
import { FinishedPanel } from './FinishedPanel'

function finishedGame(scores: [string, number][]): GameView {
  const base = aGame()
  return {
    ...base,
    phase: 'FINISHED',
    players: scores.map(([name, score]) => aPlayer(name.toLowerCase(), name, { score })),
  }
}

function renderPanel(game: GameView, send: (message: unknown) => void = () => {}) {
  return render(
    <MemoryRouter>
      <FinishedPanel game={game} send={send as never} />
    </MemoryRouter>,
  )
}

describe('FinishedPanel', () => {
  it('puts the best three on the podium, winner in the middle', () => {
    renderPanel(finishedGame([['Alice', 5], ['Bob', 9], ['Carl', 7]]))

    const steps = screen.getAllByRole('listitem')
    expect(within(steps[0]).getByText('Carl')).toBeInTheDocument()
    expect(within(steps[1]).getByText('Bob')).toBeInTheDocument()
    expect(within(steps[2]).getByText('Alice')).toBeInTheDocument()
  })

  it('lists everybody past the third place below the podium', () => {
    renderPanel(
      finishedGame([
        ['Alice', 9],
        ['Bob', 7],
        ['Carl', 5],
        ['Dave', 3],
        ['Eve', 1],
      ]),
    )

    const lists = screen.getAllByRole('list')
    const ranking = lists[lists.length - 1]
    expect(within(ranking).getByText('Dave')).toBeInTheDocument()
    expect(within(ranking).getByText('Eve')).toBeInTheDocument()
    expect(within(ranking).getByText('4')).toBeInTheDocument()
    expect(within(ranking).getByText('5')).toBeInTheDocument()
  })

  it('shows no ranking list when three players are all there is', () => {
    renderPanel(finishedGame([['Alice', 9], ['Bob', 7], ['Carl', 5]]))

    expect(screen.getAllByRole('list')).toHaveLength(1)
  })

  it('lets the host reopen the lobby for another match', async () => {
    const send = vi.fn()
    renderPanel(finishedGame([['Alice', 9], ['Bob', 7]]), send)

    screen.getByRole('button', { name: /retour au salon/i }).click()

    expect(send).toHaveBeenCalledWith({ type: 'lobby' })
  })

  it('leaves a guest waiting on the host, with nothing to press', () => {
    const game = finishedGame([['Alice', 9], ['Bob', 7]])
    game.you = { ...game.you, isHost: false }
    renderPanel(game)

    expect(screen.queryByRole('button', { name: /retour au salon/i })).not.toBeInTheDocument()
    expect(screen.queryByRole('link')).not.toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent(/en attente de l/i)
  })

  it('copes with a table of two', () => {
    renderPanel(finishedGame([['Alice', 4], ['Bob', 2]]))

    const steps = screen.getAllByRole('listitem')
    expect(steps).toHaveLength(2)
    expect(within(steps[0]).getByText('Bob')).toBeInTheDocument()
    expect(within(steps[1]).getByText('Alice')).toBeInTheDocument()
  })
})
