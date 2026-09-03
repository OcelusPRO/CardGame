import { render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { aGame } from '../../test/gameFixtures'
import { LobbyPanel } from './LobbyPanel'

// The lobby asks the server which packs the current mode allows; nothing here depends on
// the answer, so an empty list keeps the component from reaching the network.
const { packs } = vi.hoisted(() => ({ packs: vi.fn(() => Promise.resolve([])) }))
vi.mock('../../api/session', () => ({ sessionApi: { packs } }))

function lobby(isHost: boolean) {
  const base = aGame()
  return { ...base, phase: 'LOBBY' as const, you: { ...base.you, isHost } }
}

describe('LobbyPanel', () => {
  beforeEach(() => packs.mockClear())

  it('asks for the packs plainly when the host looks at the lobby', async () => {
    render(<LobbyPanel game={lobby(true)} onSettings={vi.fn()} onDeck={vi.fn()} />)

    await waitFor(() => expect(packs).toHaveBeenCalled())
    expect(packs).toHaveBeenLastCalledWith('CARDS', undefined)
  })

  it('asks for the packs as the host built them when a guest looks at the lobby', async () => {
    const game = lobby(false)
    render(<LobbyPanel game={game} onSettings={vi.fn()} onDeck={vi.fn()} />)

    await waitFor(() => expect(packs).toHaveBeenCalled())
    expect(packs).toHaveBeenLastCalledWith('CARDS', game.code)
  })

  it('offers the invitation to the host', async () => {
    render(<LobbyPanel game={lobby(true)} onSettings={vi.fn()} onDeck={vi.fn()} />)

    await waitFor(() => expect(screen.getByText('Inviter du monde')).toBeInTheDocument())
  })

  it('leaves it out entirely for everybody else', async () => {
    render(<LobbyPanel game={lobby(false)} onSettings={vi.fn()} onDeck={vi.fn()} />)

    await waitFor(() => expect(screen.getByText('Règles')).toBeInTheDocument())
    expect(screen.queryByText('Inviter du monde')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Copier le lien/i })).not.toBeInTheDocument()
  })
})
