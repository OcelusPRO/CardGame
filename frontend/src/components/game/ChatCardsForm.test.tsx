import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { ChatCardsView } from '../../api/types'
import { ChatCardsForm } from './ChatCardsForm'

function rules(overrides: Partial<ChatCardsView> = {}): ChatCardsView {
  return {
    access: 'OFF',
    situations: true,
    punchlines: true,
    minBits: 100,
    rewardId: '',
    ...overrides,
  }
}

function renderForm(settings: ChatCardsView, onChange = vi.fn()) {
  render(
    <ChatCardsForm
      settings={settings}
      disabled={false}
      lockedBecause={null}
      hostTwitchLogin="kameto"
      onChange={onChange}
    />,
  )
  return onChange
}

describe('ChatCardsForm', () => {
  it('shows nothing but the door while it is shut', () => {
    renderForm(rules())

    expect(screen.getByRole('button', { name: 'Fermé' })).toHaveAttribute('aria-pressed', 'true')
    expect(screen.queryByLabelText('Des situations')).not.toBeInTheDocument()
  })

  it('opens the chat to writing', async () => {
    const onChange = renderForm(rules())

    await userEvent.click(screen.getByRole('button', { name: 'Ouvert à tous' }))

    expect(onChange).toHaveBeenCalledWith({ access: 'EVERYONE' })
  })

  it('lets the host close one pile without closing the other', async () => {
    const onChange = renderForm(rules({ access: 'EVERYONE' }))

    await userEvent.click(screen.getByLabelText('Des situations'))

    expect(onChange).toHaveBeenCalledWith({ situations: false })
  })

  it('asks for a bits floor only in the bits mode', () => {
    const { unmount } = render(
      <ChatCardsForm
        settings={rules({ access: 'EVERYONE' })}
        disabled={false}
        lockedBecause={null}
        hostTwitchLogin="kameto"
        onChange={vi.fn()}
      />,
    )
    expect(screen.queryByLabelText('Bits minimum par carte')).not.toBeInTheDocument()
    unmount()

    renderForm(rules({ access: 'BITS' }))
    expect(screen.getByLabelText('Bits minimum par carte')).toHaveValue(100)
  })

  it('sends viewers to the chat for a channel point redemption', () => {
    renderForm(rules({ access: 'CHANNEL_POINTS' }))

    expect(screen.getByText(/récompense de points de chaîne/)).toBeInTheDocument()
    expect(screen.getByText(/« !situation … » et « !réponse … »/)).toBeInTheDocument()
  })

  it('names the channel the cards would come from', () => {
    renderForm(rules())

    expect(screen.getByText(/Le tchat de kameto écrit des cartes/)).toBeInTheDocument()
  })
})
