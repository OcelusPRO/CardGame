import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { ChannelRewardView, ChatCardsInput, ChatCardsView } from '../../api/types'
import { ChatCardsForm } from './ChatCardsForm'

// What is already standing on the channel. Every test that cares sets it explicitly.
const { twitchRewards } = vi.hoisted(() => ({
  twitchRewards: vi.fn<() => Promise<ChannelRewardView[]>>(() => Promise.resolve([])),
}))
vi.mock('../../api/session', () => ({ sessionApi: { twitchRewards } }))

function standing(overrides: Partial<ChannelRewardView> = {}): ChannelRewardView {
  return { id: 'reward-1', title: 'Une récompense', cost: 1000, managed: true, ...overrides }
}

function rules(overrides: Partial<ChatCardsView> = {}): ChatCardsView {
  return {
    access: 'OFF',
    situations: true,
    punchlines: true,
    minBits: 100,
    situationReward: { id: '', title: 'Écrire une situation', cost: 500 },
    punchlineReward: { id: '', title: 'Écrire une réponse', cost: 500 },
    ...overrides,
  }
}

interface Options {
  bitsAvailable?: boolean
  rewardsAuthorized?: boolean
  disabled?: boolean
  onChange?: (patch: ChatCardsInput) => void
}

function renderForm(settings: ChatCardsView, options: Options = {}) {
  const onChange = vi.fn(options.onChange)
  const view = render(
    <ChatCardsForm
      settings={settings}
      disabled={options.disabled ?? false}
      lockedBecause={null}
      hostTwitchLogin="kameto"
      bitsAvailable={options.bitsAvailable ?? true}
      rewardsAuthorized={options.rewardsAuthorized ?? true}
      onChange={onChange}
    />,
  )
  return { onChange, ...view }
}

const DOOR = 'Le tchat de kameto peut créer des cartes'

/** The points mode with one pile open, pointed at a reward already on the channel. */
function chatCardsOn(rewardId: string): ChatCardsView {
  return rules({
    access: 'CHANNEL_POINTS',
    punchlines: false,
    situationReward: { id: rewardId, title: '', cost: 500 },
  })
}

describe('ChatCardsForm', () => {
  beforeEach(() => {
    twitchRewards.mockReset()
    twitchRewards.mockResolvedValue([])
  })

  it('shows nothing but the door while it is shut', () => {
    renderForm(rules())

    expect(screen.getByLabelText(DOOR)).not.toBeChecked()
    expect(screen.queryByLabelText('Des situations')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Points de chaîne' })).not.toBeInTheDocument()
  })

  it('opens the chat to writing, free of charge to begin with', async () => {
    const { onChange } = renderForm(rules())

    await userEvent.click(screen.getByLabelText(DOOR))

    expect(onChange).toHaveBeenCalledWith({ access: 'EVERYONE' })
  })

  it('shuts it again in one click', async () => {
    const { onChange } = renderForm(rules({ access: 'BITS' }))

    await userEvent.click(screen.getByLabelText(DOOR))

    expect(onChange).toHaveBeenCalledWith({ access: 'OFF' })
  })

  it('lets the host close one pile without closing the other', async () => {
    const { onChange } = renderForm(rules({ access: 'EVERYONE' }))

    await userEvent.click(screen.getByLabelText('Des situations'))

    expect(onChange).toHaveBeenCalledWith({ situations: false })
  })

  it('offers the bits only when this server has an extension to cheer from', () => {
    const { unmount } = renderForm(rules({ access: 'EVERYONE' }), { bitsAvailable: false })
    expect(screen.queryByRole('button', { name: 'Bits' })).not.toBeInTheDocument()
    unmount()

    renderForm(rules({ access: 'EVERYONE' }), { bitsAvailable: true })
    expect(screen.getByRole('button', { name: 'Bits' })).toBeInTheDocument()
  })

  it('asks for a bits floor only in the bits mode', () => {
    const { unmount } = renderForm(rules({ access: 'EVERYONE' }))
    expect(screen.queryByLabelText('Bits minimum par carte')).not.toBeInTheDocument()
    unmount()

    renderForm(rules({ access: 'BITS' }))
    expect(screen.getByLabelText('Bits minimum par carte')).toHaveValue(100)
  })

  // The right rides on the Twitch sign in itself, so the only account without it is one
  // that signed in before the game started asking — signing in again is the whole fix.
  it('sends a host whose session predates the scope back through Twitch', () => {
    renderForm(rules({ access: 'CHANNEL_POINTS' }), { rewardsAuthorized: false })

    expect(screen.getByRole('link', { name: /Se reconnecter avec Twitch/ })).toHaveAttribute(
      'href',
      '/auth/twitch',
    )
    expect(screen.queryByLabelText(/Nom de la récompense/)).not.toBeInTheDocument()
  })

  it('names and prices one reward per open pile once it may', () => {
    renderForm(rules({ access: 'CHANNEL_POINTS' }))

    expect(screen.getByLabelText('Nom de la récompense « situation »')).toHaveValue(
      'Écrire une situation',
    )
    expect(screen.getByLabelText('Points de la récompense « réponse »')).toHaveValue(500)
    expect(screen.queryByRole('link', { name: /reconnecter/i })).not.toBeInTheDocument()
  })

  it('drops the reward of a pile the host closed', () => {
    renderForm(rules({ access: 'CHANNEL_POINTS', punchlines: false }))

    expect(screen.getByLabelText('Nom de la récompense « situation »')).toBeInTheDocument()
    expect(screen.queryByLabelText('Nom de la récompense « réponse »')).not.toBeInTheDocument()
  })

  // A reward is a real thing on a real channel, so what is typed only reaches the table
  // once the host stops typing — the viewers must not watch it flicker letter by letter.
  it('renames a reward once the host stops typing, and leaves the other alone', async () => {
    const { onChange } = renderForm(rules({ access: 'CHANNEL_POINTS' }))

    await userEvent.type(screen.getByLabelText('Nom de la récompense « situation »'), '!')
    expect(onChange).not.toHaveBeenCalled()

    await waitFor(() =>
      expect(onChange).toHaveBeenCalledWith({
        situationReward: { id: '', title: 'Écrire une situation!', cost: 500 },
      }),
    )
    expect(onChange).toHaveBeenCalledTimes(1)
  })

  it('offers the rewards already on the channel, and adopts the one picked', async () => {
    twitchRewards.mockResolvedValue([standing({ id: 'reward-mine', title: 'Ma carte' })])
    const { onChange } = renderForm(rules({ access: 'CHANNEL_POINTS' }))

    const picker = await screen.findByLabelText('Récompense « situation »')
    await userEvent.selectOptions(picker, 'reward-mine')

    // A pick has no next keystroke to wait for, so it goes straight through.
    expect(onChange).toHaveBeenCalledWith({
      situationReward: { id: 'reward-mine', title: 'Ma carte', cost: 1000 },
    })
    // Adopting one means there is nothing left to name or price here.
    expect(screen.queryByLabelText('Nom de la récompense « situation »')).not.toBeInTheDocument()
  })

  it('says a reward it owns will be settled and refunded on its own', async () => {
    twitchRewards.mockResolvedValue([standing({ id: 'reward-mine', managed: true })])
    renderForm(chatCardsOn('reward-mine'))

    expect(await screen.findByText(/rendra les points de celles qui sont refusées/)).toBeInTheDocument()
    expect(screen.queryByText(/il ne pourra ni valider ni/)).not.toBeInTheDocument()
  })

  // Twitch only lets the app that created a reward validate its redemptions, so the host
  // has to be told what keeping theirs actually costs them before they keep it.
  it('warns that a reward it does not own leaves the validating to the streamer', async () => {
    twitchRewards.mockResolvedValue([standing({ id: 'reward-theirs', managed: false })])
    renderForm(chatCardsOn('reward-theirs'))

    expect(await screen.findByText(/il ne pourra ni valider ni/)).toBeInTheDocument()
    expect(screen.getByText(/file d'attente Twitch/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /La recréer à l'identique/ })).toBeInTheDocument()
  })

  it('rebuilds an unmanaged reward as a new one, keeping its name and its price', async () => {
    twitchRewards.mockResolvedValue([
      standing({ id: 'reward-theirs', title: 'La leur', cost: 250, managed: false }),
    ])
    const { onChange } = renderForm(chatCardsOn('reward-theirs'))

    await userEvent.click(await screen.findByRole('button', { name: /La recréer à l'identique/ }))

    expect(onChange).toHaveBeenCalledWith({
      situationReward: { id: '', title: 'La leur', cost: 250 },
    })
  })

  it('says so while the reward being rebuilt is still standing on the channel', async () => {
    twitchRewards.mockResolvedValue([
      standing({ id: 'reward-theirs', title: 'La leur', cost: 250, managed: false }),
    ])
    renderForm(
      rules({
        access: 'CHANNEL_POINTS',
        punchlines: false,
        situationReward: { id: '', title: 'La leur', cost: 250 },
      }),
    )

    expect(await screen.findByText(/est encore sur votre chaîne/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /revérifier/i })).toBeInTheDocument()
  })

  it('never sends a guest through Twitch, and still names the rewards', () => {
    renderForm(rules({ access: 'CHANNEL_POINTS' }), {
      disabled: true,
      rewardsAuthorized: false,
    })

    expect(screen.queryByRole('link', { name: /reconnecter/i })).not.toBeInTheDocument()
    expect(screen.getByLabelText('Nom de la récompense « situation »')).toBeDisabled()
  })

  it('says the cards come from the reward box rather than the chat', () => {
    renderForm(rules({ access: 'CHANNEL_POINTS' }))

    expect(screen.getByText(/dans la case de la récompense/)).toBeInTheDocument()
    expect(screen.getByText(/retirée à la fin de la partie/)).toBeInTheDocument()
  })

  it('names the channel the cards would come from', () => {
    renderForm(rules())

    expect(screen.getByText(DOOR)).toBeInTheDocument()
  })
})
