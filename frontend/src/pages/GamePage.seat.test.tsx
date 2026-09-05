import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { GamePage } from './GamePage'
import { gamesApi } from '../api/games'
import { forgetActiveGame, readActiveGame } from '../session/activeGame'

// The socket is not what this file is about: a seated player only has to *reach* the
// table, and the store's connect is what would otherwise open a real WebSocket.
vi.mock('../game/gameStore', () => ({
  useGameStore: () => ({
    game: null,
    status: 'connecting',
    lastError: null,
    connect: vi.fn(),
    disconnect: vi.fn(),
    send: vi.fn(),
    dismissError: vi.fn(),
  }),
}))

function LocationProbe() {
  return <span data-testid="path">{useLocation().pathname}</span>
}

/** The routes exactly as App declares them: two entries rendering the same component. */
function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <LocationProbe />
      <Routes>
        <Route path="/" element={<span>accueil</span>} />
        <Route path="/create" element={<span>créer</span>} />
        <Route path="/game/:code" element={<GamePage />} />
        <Route path="/game" element={<GamePage />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('GamePage — landing on a freshly created table', () => {
  beforeEach(() => {
    forgetActiveGame()
    window.sessionStorage.clear()
    window.localStorage.clear()
  })

  afterEach(() => vi.restoreAllMocks())

  it('keeps the seat after the code is dropped from the address bar', async () => {
    // What the creator gets back: a table they are already sitting at.
    vi.spyOn(gamesApi, 'preview').mockResolvedValue({
      code: 'ABCDE',
      youArePlaying: true,
    } as never)

    renderAt('/game/ABCDE')

    // The code leaves the URL — that part is intended — so wait for it to go.
    await waitFor(() =>
      expect(screen.getByTestId('path').textContent).not.toBe('/game/ABCDE'),
    )

    // But the player must land at the table, not be bounced to the home page.
    expect(screen.getByTestId('path').textContent).toBe('/game')
    expect(screen.queryByText('accueil')).not.toBeInTheDocument()

    // And the seat has to be recorded under the real code: the bug that sent players home
    // announced itself by storing an empty string here.
    expect(readActiveGame()).toBe('ABCDE')
    await waitFor(() => expect(screen.getByText(/On installe la table/i)).toBeInTheDocument())
  })
})
