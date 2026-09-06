import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { HeaderMenu } from './HeaderMenu'
import type { MeView } from '../../api/types'

vi.mock('../../api/session', () => ({
  sessionApi: { logout: vi.fn().mockResolvedValue(undefined) },
}))

const connected: MeView = {
  playerId: 'p1',
  discordConnected: true,
  discordUsername: 'Neo',
  discordAvatarUrl: 'https://cdn.example/neo.png',
  twitchConnected: false,
  isAdmin: false,
  discordLoginAvailable: true,
  twitchLoginAvailable: true,
  twitchExtensionAvailable: false,
  twitchRewardsAuthorized: false,
}

const signedOut: MeView = {
  ...connected,
  discordConnected: false,
  discordUsername: undefined,
  discordAvatarUrl: undefined,
}

function renderMenu(me: MeView | null) {
  return render(
    <MemoryRouter>
      <HeaderMenu
        me={me}
        sound
        onToggleSound={vi.fn()}
        dark={false}
        onToggleTheme={vi.fn()}
        animate
        onToggleAnimations={vi.fn()}
      />
    </MemoryRouter>,
  )
}

describe('HeaderMenu', () => {
  beforeEach(() => {
    window.sessionStorage.clear()
  })

  it('is a burger while signed out, and holds the three switches', async () => {
    renderMenu(signedOut)

    await userEvent.click(screen.getByRole('button', { name: 'Menu' }))

    expect(screen.getByText('Sons')).toBeInTheDocument()
    expect(screen.getByText('Thème')).toBeInTheDocument()
    expect(screen.getByText('Animations')).toBeInTheDocument()
    expect(screen.getByRole('menuitem', { name: 'Se connecter' })).toBeInTheDocument()
  })

  it('shows the profile picture instead of the burger once signed in', async () => {
    const { container } = renderMenu(connected)

    const trigger = screen.getByRole('button', { name: 'Mon compte et réglages' })
    expect(container.querySelector('img')).toHaveAttribute('src', connected.discordAvatarUrl)

    await userEvent.click(trigger)
    expect(screen.getByText('Neo')).toBeInTheDocument()
    expect(screen.getByRole('menuitem', { name: 'Se déconnecter' })).toBeInTheDocument()
  })

  it('falls back to the initial when the account carries no picture', () => {
    renderMenu({ ...connected, discordAvatarUrl: undefined })

    expect(screen.getByRole('button', { name: 'Mon compte et réglages' })).toHaveTextContent('N')
  })

  it('keeps the settings reachable when there is no session at all', async () => {
    renderMenu(null)

    await userEvent.click(screen.getByRole('button', { name: 'Menu' }))
    expect(screen.getByText('Thème')).toBeInTheDocument()
    expect(screen.queryByRole('menuitem')).not.toBeInTheDocument()
  })

  it('opens the account choice from the menu', async () => {
    renderMenu(signedOut)

    await userEvent.click(screen.getByRole('button', { name: 'Menu' }))
    await userEvent.click(screen.getByRole('menuitem', { name: 'Se connecter' }))

    expect(screen.getByRole('dialog')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Discord' })).toBeInTheDocument()
  })
})
