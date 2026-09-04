import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AdultAccessEditor } from './AdultAccessEditor'

const { adultAccess, addAdultAccess, removeAdultAccess } = vi.hoisted(() => ({
  adultAccess: vi.fn(),
  addAdultAccess: vi.fn(),
  removeAdultAccess: vi.fn(),
}))
vi.mock('../../api/admin', () => ({
  adminApi: { adultAccess, addAdultAccess, removeAdultAccess },
}))

describe('AdultAccessEditor', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    adultAccess.mockResolvedValue([
      { provider: 'DISCORD', accountId: '100000000000000042', label: 'Alex', addedAtMillis: 1 },
      { provider: 'TWITCH', accountId: '44322889', label: 'Kameto', addedAtMillis: 2 },
    ])
    addAdultAccess.mockResolvedValue(undefined)
    removeAdultAccess.mockResolvedValue(undefined)
  })

  it('says which provider each id belongs to', async () => {
    render(<AdultAccessEditor />)

    expect(await screen.findByText(/Discord · 100000000000000042/)).toBeInTheDocument()
    expect(screen.getByText(/Twitch · 44322889/)).toBeInTheDocument()
  })

  it('adds a Discord account by default', async () => {
    render(<AdultAccessEditor />)
    await screen.findByText(/Alex/)

    await userEvent.type(screen.getByLabelText(/Identifiant numérique/i), '123')
    await userEvent.click(screen.getByRole('button', { name: 'Ajouter' }))

    expect(addAdultAccess).toHaveBeenCalledWith({
      provider: 'DISCORD',
      accountId: '123',
      label: '',
    })
  })

  it('adds a Twitch account once that side is picked', async () => {
    render(<AdultAccessEditor />)
    await screen.findByText(/Alex/)

    await userEvent.click(screen.getByRole('button', { name: 'Twitch' }))
    await userEvent.type(screen.getByLabelText(/Identifiant numérique/i), '44322889')
    await userEvent.type(screen.getByLabelText(/Nom/i), 'Kameto')
    await userEvent.click(screen.getByRole('button', { name: 'Ajouter' }))

    expect(addAdultAccess).toHaveBeenCalledWith({
      provider: 'TWITCH',
      accountId: '44322889',
      label: 'Kameto',
    })
  })

  it('removes an entry with the provider it belongs to', async () => {
    render(<AdultAccessEditor />)
    await screen.findByText(/Kameto/)

    await userEvent.click(screen.getByRole('button', { name: 'Retirer Kameto' }))

    await waitFor(() => expect(removeAdultAccess).toHaveBeenCalledWith('TWITCH', '44322889'))
  })
})
