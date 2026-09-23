import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { aGame, aPlayer } from '../../test/gameFixtures'
import { StepAsidePanel } from './StepAsidePanel'

const lobby = aGame({
  phase: 'LOBBY',
  players: [aPlayer('alice', 'Alice'), aPlayer('bob', 'Bob'), aPlayer('phone', 'Mon tel')],
})

describe('StepAsidePanel', () => {
  it('hands the crown to the last seat taken by default', async () => {
    const onStepAside = vi.fn()
    render(<StepAsidePanel game={lobby} onStepAside={onStepAside} />)

    await userEvent.click(screen.getByRole('button', { name: /Passer en spectateur/ }))
    await userEvent.click(screen.getByRole('button', { name: 'Passer en spectateur' }))

    expect(onStepAside).toHaveBeenCalledWith('phone')
  })

  it('lets the host pick another heir', async () => {
    const onStepAside = vi.fn()
    render(<StepAsidePanel game={lobby} onStepAside={onStepAside} />)

    await userEvent.click(screen.getByRole('button', { name: /Passer en spectateur/ }))
    await userEvent.click(screen.getByRole('button', { name: 'Bob' }))
    await userEvent.click(screen.getByRole('button', { name: 'Passer en spectateur' }))

    expect(onStepAside).toHaveBeenCalledWith('bob')
  })

  it('cannot step aside alone at the table', async () => {
    render(<StepAsidePanel game={aGame({ phase: 'LOBBY', players: [aPlayer('alice', 'Alice')] })} onStepAside={vi.fn()} />)

    await userEvent.click(screen.getByRole('button', { name: /Passer en spectateur/ }))

    expect(screen.getByRole('button', { name: 'Passer en spectateur' })).toBeDisabled()
  })
})
