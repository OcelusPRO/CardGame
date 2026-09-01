import { render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { aGame } from '../../test/gameFixtures'
import { LobbyPanel } from './LobbyPanel'

// The lobby asks the server which packs the current mode allows; nothing here depends on
// the answer, so an empty list keeps the component from reaching the network.
vi.mock('../../api/session', () => ({ sessionApi: { packs: () => Promise.resolve([]) } }))

function lobby(isHost: boolean) {
  const base = aGame()
  return { ...base, phase: 'LOBBY' as const, you: { ...base.you, isHost } }
}

describe('LobbyPanel', () => {
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
