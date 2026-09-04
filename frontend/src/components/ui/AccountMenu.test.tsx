import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AccountMenu } from './AccountMenu'
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
  twitchConnected: false,
  isAdmin: false,
  discordLoginAvailable: true,
  twitchLoginAvailable: true,
}

const signedOut: MeView = { ...connected, discordConnected: false, discordUsername: undefined }

function renderMenu(me: MeView | null) {
  return render(
    <MemoryRouter>
      <AccountMenu me={me} />
    </MemoryRouter>,
  )
}

const signIn = () => screen.getByRole('button', { name: 'Se connecter' })

describe('AccountMenu', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.sessionStorage.clear()
  })

  it('stays hidden when no sign in is configured', () => {
    const { container } = renderMenu({
      ...signedOut,
      discordLoginAvailable: false,
      twitchLoginAvailable: false,
    })
    expect(container).toBeEmptyDOMElement()
  })

  it('is a single button while signed out', () => {
    renderMenu(signedOut)

    expect(signIn()).toBeInTheDocument()
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('offers both accounts once it is opened', async () => {
    renderMenu(signedOut)

    await userEvent.click(signIn())

    expect(screen.getByRole('dialog')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Discord/i })).toHaveAttribute('href', '/auth/discord')
    expect(screen.getByRole('link', { name: /Twitch/i })).toHaveAttribute('href', '/auth/twitch')
  })

  it('only offers what the server actually supports', async () => {
    renderMenu({ ...signedOut, twitchLoginAvailable: false })

    await userEvent.click(signIn())

    expect(screen.queryByRole('link', { name: /Twitch/i })).not.toBeInTheDocument()
  })

  it('closes without signing anybody in', async () => {
    renderMenu(signedOut)
    await userEvent.click(signIn())

    await userEvent.click(screen.getByRole('button', { name: /Plus tard/i }))

    // The dialog animates out, so it lingers for a frame after the click.
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
  })

  it('remembers where the player was before the redirect', async () => {
    renderMenu(signedOut)
    await userEvent.click(signIn())

    await userEvent.click(screen.getByRole('link', { name: /Twitch/i }))

    expect(window.sessionStorage.getItem('sansfiltres:auth-return')).toBe('/')
  })

  it('keeps the menu closed until the profile is clicked', () => {
    renderMenu(connected)
    expect(screen.queryByRole('menu')).not.toBeInTheDocument()
  })

  it('reveals a logout action from the profile', async () => {
    renderMenu(connected)
    await userEvent.click(screen.getByRole('button', { name: /Neo/i }))
    expect(screen.getByRole('menuitem', { name: /Se déconnecter/i })).toBeInTheDocument()
  })

  it('never asks a signed-in player to sign in again', () => {
    renderMenu(connected)
    expect(screen.queryByRole('button', { name: 'Se connecter' })).not.toBeInTheDocument()
  })

  it('shows the Twitch name and picture when Twitch is the account', () => {
    renderMenu({
      ...signedOut,
      twitchConnected: true,
      twitchUsername: 'Kameto',
      twitchLogin: 'kameto',
      twitchAvatarUrl: 'https://static-cdn.jtvnw.net/jtv_user_pictures/kameto.png',
    })

    expect(screen.getByRole('button', { name: /Kameto/i })).toBeInTheDocument()
    expect(document.querySelector('img')).toHaveAttribute(
      'src',
      'https://static-cdn.jtvnw.net/jtv_user_pictures/kameto.png',
    )
  })

  it('hides the admin shortcut for non-admins', async () => {
    renderMenu(connected)
    await userEvent.click(screen.getByRole('button', { name: /Neo/i }))
    expect(screen.queryByRole('menuitem', { name: /administration/i })).not.toBeInTheDocument()
  })

  it('shows the admin shortcut for admins', async () => {
    renderMenu({ ...connected, isAdmin: true })
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

      renderMenu(connected)
      await userEvent.click(screen.getByRole('button', { name: /Neo/i }))
      await userEvent.click(screen.getByRole('menuitem', { name: /Se déconnecter/i }))

      expect(sessionApi.logout).toHaveBeenCalledOnce()
      expect(hrefSetter).toHaveBeenCalledWith('/')
    })
  })
})
