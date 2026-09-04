import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { GameView } from '../../api/types'
import { aGame } from '../../test/gameFixtures'
import { writtenIntoSituation } from '../../test/writtenAnswer'
import { VotePanel } from './VotePanel'

function votingGame(overrides: Partial<GameView> = {}): GameView {
  const base = aGame()
  return {
    ...base,
    phase: 'SELECTING',
    you: { ...base.you, mustAnswer: false, mustVote: true },
    round: {
      ...base.round!,
      answers: [
        { id: 0, texts: ['un chat mouillé'], filledText: "Le pire, c'est un chat mouillé.", isMine: true },
        { id: 1, texts: ['la honte'], filledText: "Le pire, c'est la honte.", isMine: false },
      ],
    },
    ...overrides,
  }
}

describe('VotePanel', () => {
  it('sends the vote for the answer that was clicked', async () => {
    const onChoose = vi.fn()
    render(<VotePanel game={votingGame()} onChoose={onChoose} />)

    await userEvent.click(screen.getByRole('button', { name: 'la honte' }))

    expect(onChoose).toHaveBeenCalledWith(1)
  })

  it('never lets a player vote for their own answer', async () => {
    const onChoose = vi.fn()
    render(<VotePanel game={votingGame()} onChoose={onChoose} />)

    await userEvent.click(screen.getByText('un chat mouillé'))

    expect(onChoose).not.toHaveBeenCalled()
  })

  it('lets a player vote for their own answer once the host allows it', async () => {
    const onChoose = vi.fn()
    const game = votingGame()
    render(
      <VotePanel
        game={{ ...game, settings: { ...game.settings, allowSelfVote: true } }}
        onChoose={onChoose}
      />,
    )

    await userEvent.click(screen.getByRole('button', { name: 'un chat mouillé' }))

    expect(onChoose).toHaveBeenCalledWith(0)
  })

  it('still blocks self-vote in czar mode even if the flag is on', async () => {
    const onChoose = vi.fn()
    const game = votingGame()
    render(
      <VotePanel
        game={{
          ...game,
          settings: { ...game.settings, allowSelfVote: true, selectionMode: 'CZAR' },
        }}
        onChoose={onChoose}
      />,
    )

    await userEvent.click(screen.getByText('un chat mouillé'))

    expect(onChoose).not.toHaveBeenCalled()
  })

  it('writes the pointed answer into the situation card', async () => {
    render(<VotePanel game={votingGame()} onChoose={vi.fn()} />)
    expect(writtenIntoSituation('la honte')).toHaveLength(0)

    await userEvent.hover(screen.getByRole('button', { name: 'la honte' }))

    expect(writtenIntoSituation('la honte')).toHaveLength(1)
  })

  it('keeps showing the answer it voted for', () => {
    const game = votingGame()
    render(
      <VotePanel game={{ ...game, round: { ...game.round!, myVote: 1 } }} onChoose={vi.fn()} />,
    )

    expect(screen.getByText('la honte')).toBeInTheDocument()
    expect(writtenIntoSituation('la honte')).toHaveLength(1)
  })

  it('keeps the authors hidden while the vote is open', () => {
    render(<VotePanel game={votingGame()} onChoose={vi.fn()} />)

    expect(screen.queryByText('Bob')).not.toBeInTheDocument()
    expect(screen.getByText('Votez pour la meilleure réponse.')).toBeInTheDocument()
  })

  it('says the vote is in once it has been cast', () => {
    const game = votingGame()
    render(<VotePanel game={{ ...game, you: { ...game.you, mustVote: false } }} onChoose={vi.fn()} />)

    expect(screen.getByText(/Vote enregistré/i)).toBeInTheDocument()
  })

  describe('when a Twitch chat votes too', () => {
    function chatGame(): GameView {
      const game = votingGame()
      return {
        ...game,
        chatChannels: ['kameto'],
        settings: { ...game.settings, twitchChatVote: true },
        round: { ...game.round!, chatVotes: { '1': 12 } },
      }
    }

    it('tells the viewers what to type, and where', () => {
      render(<VotePanel game={chatGame()} onChoose={vi.fn()} />)

      expect(screen.getByText(/Tapez le numéro de la réponse/i)).toBeInTheDocument()
      expect(screen.getByText('kameto')).toBeInTheDocument()
    })

    it('numbers every answer the way the chat must type it', () => {
      render(<VotePanel game={chatGame()} onChoose={vi.fn()} />)

      expect(screen.getByText('1')).toBeInTheDocument()
      expect(screen.getByText('2')).toBeInTheDocument()
    })

    it('counts the chat live, on the answer and in the notice', () => {
      render(<VotePanel game={chatGame()} onChoose={vi.fn()} />)

      expect(screen.getByText('12 tchat')).toBeInTheDocument()
      expect(screen.getByText('0 tchat')).toBeInTheDocument()
      expect(screen.getByText(/12 vote\(s\) du tchat/i)).toBeInTheDocument()
    })

    it('says nothing about a chat when none is being read', () => {
      render(<VotePanel game={votingGame()} onChoose={vi.fn()} />)

      expect(screen.queryByText(/Tapez le numéro/i)).not.toBeInTheDocument()
      expect(screen.queryByText(/tchat/i)).not.toBeInTheDocument()
    })
  })

  it('hands the decision to the czar in czar mode', () => {
    const game = votingGame()
    render(
      <VotePanel
        game={{ ...game, settings: { ...game.settings, selectionMode: 'CZAR' } }}
        onChoose={vi.fn()}
      />,
    )

    expect(screen.getByText('À vous de trancher.')).toBeInTheDocument()
  })
})
