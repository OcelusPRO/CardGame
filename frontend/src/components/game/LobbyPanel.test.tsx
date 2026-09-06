import { render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { MeView } from '../../api/types'
import { aGame, aPlayer } from '../../test/gameFixtures'
import { LobbyPanel } from './LobbyPanel'

// The lobby asks the server which packs the current mode allows; nothing here depends on
// the answer, so an empty list keeps the component from reaching the network.
const { packs } = vi.hoisted(() => ({ packs: vi.fn(() => Promise.resolve([])) }))
vi.mock('../../api/session', () => ({ sessionApi: { packs } }))

function lobby(isHost: boolean) {
  const base = aGame()
  return { ...base, phase: 'LOBBY' as const, you: { ...base.you, isHost } }
}

/** The same lobby, hosted by somebody who signed in with Twitch. */
function streamed(isHost = true) {
  const base = lobby(isHost)
  return {
    ...base,
    players: [aPlayer('alice', 'Alice', { twitchLogin: 'kameto' }), aPlayer('bob', 'Bob')],
  }
}

function me(overrides: Partial<MeView> = {}): MeView {
  return {
    playerId: 'alice',
    discordConnected: false,
    twitchConnected: true,
    twitchLogin: 'kameto',
    isAdmin: false,
    discordLoginAvailable: false,
    twitchLoginAvailable: true,
    twitchExtensionAvailable: false,
    twitchRewardsAuthorized: false,
    ...overrides,
  }
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

  // Where the viewers write cards is a question about the paquet, not about the rules,
  // so it sits beside the packs rather than beside the timers.
  it('puts the chat writing cards in the paquet, not in the rules', async () => {
    render(<LobbyPanel game={streamed()} me={me()} onSettings={vi.fn()} onDeck={vi.fn()} />)

    const door = await screen.findByLabelText('Le tchat de kameto peut créer des cartes')
    const paquet = screen.getByText('Paquet de cartes').closest('section')
    const regles = screen.getByText('Règles').closest('section')

    expect(paquet).toContainElement(door)
    expect(regles).not.toContainElement(door)
  })

  it('says nothing about it when the host never signed in with Twitch', async () => {
    render(<LobbyPanel game={lobby(true)} me={me()} onSettings={vi.fn()} onDeck={vi.fn()} />)

    await waitFor(() => expect(screen.getByText('Paquet de cartes')).toBeInTheDocument())
    expect(screen.queryByLabelText(/peut créer des cartes/)).not.toBeInTheDocument()
  })

  it('offers the bits only when the server has an extension', async () => {
    const game = { ...streamed(), settings: chatCards(streamed(), 'EVERYONE') }
    const { unmount } = render(
      <LobbyPanel game={game} me={me()} onSettings={vi.fn()} onDeck={vi.fn()} />,
    )
    await waitFor(() => expect(screen.getByRole('button', { name: 'Ouvert à tous' })).toBeInTheDocument())
    expect(screen.queryByRole('button', { name: 'Bits' })).not.toBeInTheDocument()
    unmount()

    render(
      <LobbyPanel
        game={game}
        me={me({ twitchExtensionAvailable: true })}
        onSettings={vi.fn()}
        onDeck={vi.fn()}
      />,
    )
    await waitFor(() => expect(screen.getByRole('button', { name: 'Bits' })).toBeInTheDocument())
  })
})

/** The same settings, with the chat opened to writing one way or another. */
function chatCards(game: ReturnType<typeof streamed>, access: 'EVERYONE' | 'CHANNEL_POINTS') {
  return { ...game.settings, chatCards: { ...game.settings.chatCards, access } }
}
