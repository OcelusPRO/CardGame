import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { GameSettingsView } from '../../api/types'
import { aGame } from '../../test/gameFixtures'
import { SettingsForm } from './SettingsForm'

function settings(overrides: Partial<GameSettingsView> = {}): GameSettingsView {
  return { ...aGame().settings, ...overrides }
}

describe('SettingsForm', () => {
  describe('the Twitch chat as a way of judging', () => {
    it('is not offered until the host signs in with Twitch', () => {
      render(<SettingsForm settings={settings()} disabled={false} onChange={vi.fn()} />)

      expect(screen.queryByText(/tchat/i)).not.toBeInTheDocument()
    })

    it('sits next to the other two, named after the channel', () => {
      render(
        <SettingsForm
          settings={settings()}
          disabled={false}
          hostTwitchLogin="kameto"
          onChange={vi.fn()}
        />,
      )

      expect(screen.getByRole('button', { name: /Le tchat de kameto vote/i })).toBeInTheDocument()
    })

    it('hands the whole vote over to the chat', async () => {
      const onChange = vi.fn()
      render(
        <SettingsForm
          settings={settings()}
          disabled={false}
          hostTwitchLogin="kameto"
          onChange={onChange}
        />,
      )

      await userEvent.click(screen.getByRole('button', { name: /Le tchat de kameto/i }))

      expect(onChange).toHaveBeenCalledWith({ selectionMode: 'CHAT' })
    })

    it('is switched off again by picking another way of judging', async () => {
      const onChange = vi.fn()
      render(
        <SettingsForm
          settings={settings({ selectionMode: 'CHAT' })}
          disabled={false}
          hostTwitchLogin="kameto"
          onChange={onChange}
        />,
      )

      await userEvent.click(screen.getByRole('button', { name: 'Maître du jeu tournant' }))

      expect(onChange).toHaveBeenCalledWith({ selectionMode: 'CZAR' })
    })

    it('shows as the chosen one while it is on', () => {
      render(
        <SettingsForm
          settings={settings({ selectionMode: 'CHAT' })}
          disabled={false}
          hostTwitchLogin="kameto"
          onChange={vi.fn()}
        />,
      )

      expect(screen.getByRole('button', { name: /Le tchat de kameto/i })).toHaveAttribute(
        'aria-pressed',
        'true',
      )
    })

    it('only then offers the chats of the other players', async () => {
      const onChange = vi.fn()
      const { rerender } = render(
        <SettingsForm
          settings={settings()}
          disabled={false}
          hostTwitchLogin="kameto"
          guestTwitchLogins={['ponce', 'zerator']}
          onChange={onChange}
        />,
      )
      expect(screen.queryByRole('checkbox', { name: /Inclure les tchats/i })).not.toBeInTheDocument()

      rerender(
        <SettingsForm
          settings={settings({ selectionMode: 'CHAT' })}
          disabled={false}
          hostTwitchLogin="kameto"
          guestTwitchLogins={['ponce', 'zerator']}
          onChange={onChange}
        />,
      )
      await userEvent.click(screen.getByRole('checkbox', { name: /ponce, zerator/i }))

      expect(onChange).toHaveBeenCalledWith({ twitchGuestChats: true })
    })
  })

  describe('rules that cannot apply', () => {
    it('drops the hand size in the write-your-own mode', () => {
      const { rerender } = render(
        <SettingsForm settings={settings()} disabled={false} onChange={vi.fn()} />,
      )
      expect(screen.getByLabelText('Cartes en main')).toBeInTheDocument()

      rerender(
        <SettingsForm
          settings={settings({ answerMode: 'FREE_TEXT' })}
          disabled={false}
          onChange={vi.fn()}
        />,
      )

      expect(screen.queryByLabelText('Cartes en main')).not.toBeInTheDocument()
    })

    it('drops everything about voting when a czar decides alone', () => {
      render(
        <SettingsForm
          settings={settings({ selectionMode: 'CZAR' })}
          disabled={false}
          hostTwitchLogin="kameto"
          onChange={vi.fn()}
        />,
      )

      expect(screen.queryByLabelText('Bonus unanimité')).not.toBeInTheDocument()
      expect(screen.queryByRole('checkbox', { name: /sa propre carte/i })).not.toBeInTheDocument()
      expect(screen.getByRole('checkbox', { name: /Le maître du jeu répond aussi/i })).toBeInTheDocument()
    })

    it('drops every point rule when the chat judges alone', () => {
      render(
        <SettingsForm
          settings={settings({ selectionMode: 'CHAT' })}
          disabled={false}
          hostTwitchLogin="kameto"
          onChange={vi.fn()}
        />,
      )

      expect(screen.queryByRole('checkbox', { name: /sa propre carte/i })).not.toBeInTheDocument()
      // The most voted answer wins the round, and a round is worth one point.
      expect(screen.queryByLabelText('Bonus unanimité')).not.toBeInTheDocument()
      expect(screen.queryByLabelText('Points par vote')).not.toBeInTheDocument()
      expect(screen.getByText(/une manche vaut 1 point/i)).toBeInTheDocument()
    })

    it("keeps the czar's own rule out of the everybody-votes mode", () => {
      render(<SettingsForm settings={settings()} disabled={false} onChange={vi.fn()} />)

      expect(screen.queryByRole('checkbox', { name: /Le maître du jeu répond aussi/i })).not.toBeInTheDocument()
      expect(screen.getByRole('checkbox', { name: /sa propre carte/i })).toBeInTheDocument()
    })
  })

  it('leaves every rule to the host', () => {
    render(
      <SettingsForm settings={settings()} disabled hostTwitchLogin="kameto" onChange={vi.fn()} />,
    )

    expect(screen.getByRole('button', { name: 'Tout le monde vote' })).toBeDisabled()
    expect(screen.getByRole('checkbox', { name: /sa propre carte/i })).toBeDisabled()
  })
})
