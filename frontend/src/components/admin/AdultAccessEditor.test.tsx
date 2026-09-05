import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AdultAccessEditor } from './AdultAccessEditor'

const { adultAccess, addAdultAccess, removeAdultAccess, findAccount } = vi.hoisted(() => ({
  adultAccess: vi.fn(),
  addAdultAccess: vi.fn(),
  removeAdultAccess: vi.fn(),
  findAccount: vi.fn(),
}))
vi.mock('../../api/admin', () => ({
  adminApi: { adultAccess, addAdultAccess, removeAdultAccess, findAccount },
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
    findAccount.mockRejectedValue(new Error('nobody'))
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

  it('takes a Twitch channel name and lets the server resolve it', async () => {
    render(<AdultAccessEditor />)
    await screen.findByText(/Alex/)

    await userEvent.click(screen.getByRole('button', { name: 'Twitch' }))
    await userEvent.type(screen.getByLabelText(/Chaîne ou identifiant/i), 'kameto')
    await userEvent.click(screen.getByRole('button', { name: 'Ajouter' }))

    expect(addAdultAccess).toHaveBeenCalledWith({
      provider: 'TWITCH',
      accountId: 'kameto',
      label: '',
    })
  })

  it('shows who is behind what is being typed', async () => {
    findAccount.mockResolvedValue({
      provider: 'TWITCH',
      accountId: '44322889',
      name: 'Kameto',
      login: 'kameto',
      avatarUrl: 'https://static-cdn.jtvnw.net/kameto.png',
    })
    render(<AdultAccessEditor />)
    await screen.findByText(/Alex/)

    await userEvent.click(screen.getByRole('button', { name: 'Twitch' }))
    await userEvent.type(screen.getByLabelText(/Chaîne ou identifiant/i), 'kameto')

    await waitFor(() => expect(findAccount).toHaveBeenCalledWith('TWITCH', 'kameto'))
    // The name is already in the list above, so the face is what tells the preview apart.
    expect(await screen.findByAltText('Kameto')).toHaveAttribute(
      'src',
      'https://static-cdn.jtvnw.net/kameto.png',
    )
    expect(screen.getAllByText('Kameto')).toHaveLength(2)
  })

  it('says nothing when the account cannot be found', async () => {
    render(<AdultAccessEditor />)
    await screen.findByText(/Alex/)

    await userEvent.type(screen.getByLabelText(/Identifiant numérique/i), '999')

    await waitFor(() => expect(findAccount).toHaveBeenCalled())
    expect(screen.queryByRole('img')).not.toBeInTheDocument()
  })

  it('removes an entry with the provider it belongs to', async () => {
    render(<AdultAccessEditor />)
    await screen.findByText(/Kameto/)

    await userEvent.click(screen.getByRole('button', { name: 'Retirer Kameto' }))

    await waitFor(() => expect(removeAdultAccess).toHaveBeenCalledWith('TWITCH', '44322889'))
  })
})
