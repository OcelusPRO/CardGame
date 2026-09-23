import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { GameView } from '../../api/types'
import { aGame } from '../../test/gameFixtures'
import { SharedDeviceBoard } from './SharedDeviceBoard'

/** A shared device at the answering step, both players still to play. */
function onTheSofa(overrides: Partial<GameView> = {}): GameView {
  return aGame({ sharedDevice: true, awaiting: ['alice', 'bob'], ...overrides })
}

describe('SharedDeviceBoard', () => {
  it('asks to pass the phone, and shows no hand until its owner claims it', async () => {
    const send = vi.fn()
    render(<SharedDeviceBoard game={onTheSofa({ seat: undefined })} send={send} />)

    expect(screen.getByText('Alice')).toBeInTheDocument()
    expect(screen.queryByText('un chat mouillé')).not.toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: /C'est moi, Alice/ }))
    expect(send).toHaveBeenCalledWith({ type: 'seat', playerId: 'alice' })
  })

  it('lets whoever is closest take the phone instead', async () => {
    const send = vi.fn()
    render(<SharedDeviceBoard game={onTheSofa()} send={send} />)

    await userEvent.click(screen.getByRole('button', { name: 'Bob' }))

    expect(send).toHaveBeenCalledWith({ type: 'seat', playerId: 'bob' })
  })

  it('shows the hand of the player holding the phone', () => {
    render(<SharedDeviceBoard game={onTheSofa({ seat: 'alice' })} send={vi.fn()} />)

    expect(screen.getByText(/C'est au tour de/)).toBeInTheDocument()
    expect(screen.getAllByText('un chat mouillé').length).toBeGreaterThan(0)
  })

  it('hides the hand of whoever just played and names the next player', () => {
    render(<SharedDeviceBoard game={onTheSofa({ seat: 'alice', awaiting: ['bob'] })} send={vi.fn()} />)

    expect(screen.getByText(/C'est noté, Alice/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /C'est moi, Bob/ })).toBeInTheDocument()
    expect(screen.queryByText('un chat mouillé')).not.toBeInTheDocument()
  })

  it('turns the phone back to the room once the step waits for nobody', () => {
    const send = vi.fn()
    render(
      <SharedDeviceBoard
        game={onTheSofa({ phase: 'ROUND_RESULT', seat: 'bob', awaiting: [] })}
        send={send}
      />,
    )

    expect(send).toHaveBeenCalledWith({ type: 'seat', playerId: null })
    expect(screen.queryByText('un chat mouillé')).not.toBeInTheDocument()
  })
})
