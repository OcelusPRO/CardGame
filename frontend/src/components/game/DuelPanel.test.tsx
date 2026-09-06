import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { BracketView, GameView, SelectionMode } from '../../api/types'
import { aGame } from '../../test/gameFixtures'
import { DuelPanel } from './DuelPanel'

function duelGame(
  bracket: Partial<BracketView> = {},
  overrides: Partial<GameView> = {},
  mode: SelectionMode = 'VOTE',
): GameView {
  const base = aGame()
  return {
    ...base,
    phase: 'SELECTING',
    settings: { ...base.settings, selectionMode: mode, selectionFormat: 'DUELS' },
    you: { ...base.you, mustAnswer: false, mustVote: true },
    round: {
      ...base.round!,
      answers: [
        { id: 0, texts: ['un chat mouillé'], filledText: "Le pire, c'est un chat mouillé.", isMine: true },
        { id: 1, texts: ['la honte'], filledText: "Le pire, c'est la honte.", isMine: false },
        { id: 2, texts: ['le silence'], filledText: "Le pire, c'est le silence.", isMine: false },
        { id: 3, texts: ['un lundi'], filledText: "Le pire, c'est un lundi.", isMine: false },
      ],
      bracket: {
        tier: 1,
        duelNumber: 1,
        duelCount: 2,
        left: 0,
        right: 1,
        wins: [],
        ...bracket,
      },
    },
    ...overrides,
  }
}

describe('DuelPanel', () => {
  it('puts only the two answers of the open duel on the table', () => {
    render(<DuelPanel game={duelGame()} onChoose={vi.fn()} />)

    expect(screen.getByText('un chat mouillé')).toBeInTheDocument()
    expect(screen.getByText('la honte')).toBeInTheDocument()
    expect(screen.queryByText('le silence')).not.toBeInTheDocument()
    expect(screen.queryByText('un lundi')).not.toBeInTheDocument()
  })

  it('sends the vote for the answer that was clicked', async () => {
    const onChoose = vi.fn()
    render(<DuelPanel game={duelGame()} onChoose={onChoose} />)

    await userEvent.click(screen.getByRole('button', { name: 'la honte' }))

    expect(onChoose).toHaveBeenCalledWith(1)
  })

  it('never lets a player vote for their own answer', async () => {
    const onChoose = vi.fn()
    render(<DuelPanel game={duelGame()} onChoose={onChoose} />)

    await userEvent.click(screen.getByText('un chat mouillé'))

    expect(onChoose).not.toHaveBeenCalled()
  })

  it('says where the table is in the ladder', () => {
    render(<DuelPanel game={duelGame({ duelNumber: 2, duelCount: 2 })} onChoose={vi.fn()} />)

    expect(screen.getByText(/Demi-finales/)).toBeInTheDocument()
    expect(screen.getByText(/duel 2 \/ 2/)).toBeInTheDocument()
  })

  it('calls the last tier a final, whatever number it carries', () => {
    render(
      <DuelPanel
        game={duelGame({ tier: 3, duelNumber: 1, duelCount: 1, left: 0, right: 2 })}
        onChoose={vi.fn()}
      />,
    )

    expect(screen.getByText(/Finale/)).toBeInTheDocument()
  })

  it('serves the czar the same two answers, and says who is deciding', () => {
    render(<DuelPanel game={duelGame({}, {}, 'CZAR')} onChoose={vi.fn()} />)

    expect(screen.getByText(/À vous de trancher/)).toBeInTheDocument()
    expect(screen.getByText('la honte')).toBeInTheDocument()
  })

  it('numbers the two sides 1 and 2 for a chat, whatever their answer ids are', () => {
    const game = duelGame({ left: 2, right: 3 }, { chatChannels: ['kameto'] }, 'CHAT')
    render(<DuelPanel game={{ ...game, you: { ...game.you, mustVote: false } }} onChoose={vi.fn()} />)

    expect(screen.getByText(/Le tchat départage/)).toBeInTheDocument()
    expect(screen.getByText('1')).toBeInTheDocument()
    expect(screen.getByText('2')).toBeInTheDocument()
    expect(screen.queryByText('3')).not.toBeInTheDocument()
  })

  it('counts only the voices of the duel on the table', () => {
    const game = duelGame({}, { chatChannels: ['kameto'] }, 'CHAT')
    render(
      <DuelPanel
        game={{ ...game, you: { ...game.you, mustVote: false } }}
        onChoose={vi.fn()}
        liveChatVotes={{ 0: { count: 12, voters: [] }, 1: { count: 30, voters: [] } }}
      />,
    )

    expect(screen.getByText(/42 vote\(s\) du tchat/)).toBeInTheDocument()
  })

  it('draws nothing at all when the round carries no ladder', () => {
    const game = duelGame()
    const { container } = render(
      <DuelPanel game={{ ...game, round: { ...game.round!, bracket: undefined } }} onChoose={vi.fn()} />,
    )

    expect(container).toBeEmptyDOMElement()
  })
})
