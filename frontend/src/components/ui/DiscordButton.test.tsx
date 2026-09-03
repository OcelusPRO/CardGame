import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { DiscordButton } from './DiscordButton'
import { sessionApi } from '../../api/session'
import type { MeView } from '../../api/types'

vi.mock('../../api/session', () => ({
  sessionApi: { logout: vi.fn().mockResolvedValue(undefined) },
}))

const connected: MeView = {
  playerId: 'p1',
  discordConnected: true,
  discordUsername: 'Neo',
  discordAvatarUrl: undefined,
  isAdmin: false,
  discordLoginAvailable: true,
}

function renderButton(me: MeView | null) {
  return render(
    <MemoryRouter>
      <DiscordButton me={me} />
    </MemoryRouter>,
  )
}

describe('DiscordButton', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('stays hidden when Discord sign in is not configured', () => {
    const { container } = renderButton({ ...connected, discordLoginAvailable: false })
    expect(container).toBeEmptyDOMElement()
  })

  it('offers the sign-in link while signed out', () => {
    renderButton({ ...connected, discordConnected: false })
    expect(screen.getByRole('link', { name: /Connexion Discord/i })).toHaveAttribute('href', '/auth/discord')
  })

  it('keeps the menu closed until the profile is clicked', () => {
    renderButton(connected)
    expect(screen.queryByRole('menu')).not.toBeInTheDocument()
  })

  it('reveals a logout action from the profile', async () => {
    renderButton(connected)
    await userEvent.click(screen.getByRole('button', { name: /Neo/i }))
    expect(screen.getByRole('menuitem', { name: /Se déconnecter/i })).toBeInTheDocument()
  })

  it('hides the admin shortcut for non-admins', async () => {
    renderButton(connected)
    await userEvent.click(screen.getByRole('button', { name: /Neo/i }))
    expect(screen.queryByRole('menuitem', { name: /administration/i })).not.toBeInTheDocument()
  })

  it('shows the admin shortcut for admins', async () => {
    renderButton({ ...connected, isAdmin: true })
    await userEvent.click(screen.getByRole('button', { name: /Neo/i }))
    expect(screen.getByRole('menuitem', { name: /Espace administration/i })).toHaveAttribute('href', '/admin')
  })

  describe('when logging out', () => {
    const realLocation = window.location

    afterEach(() => {
      Object.defineProperty(window, 'location', { configurable: true, value: realLocation })
    })

    it('clears the session and returns home', async () => {
      const hrefSetter = vi.fn()
      Object.defineProperty(window, 'location', {
        configurable: true,
        value: {
          get href() {
            return ''
          },
          set href(value: string) {
            hrefSetter(value)
          },
        },
      })

      renderButton(connected)
      await userEvent.click(screen.getByRole('button', { name: /Neo/i }))
      await userEvent.click(screen.getByRole('menuitem', { name: /Se déconnecter/i }))

      expect(sessionApi.logout).toHaveBeenCalledOnce()
      expect(hrefSetter).toHaveBeenCalledWith('/')
    })
  })
})
