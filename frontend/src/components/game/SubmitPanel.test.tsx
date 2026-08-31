import { act, fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { aGame } from '../../test/gameFixtures'
import { SubmitPanel } from './SubmitPanel'

describe('SubmitPanel', () => {
  afterEach(() => vi.useRealTimers())

  it('waits for the right number of cards before allowing a send', async () => {
    const onPlayCards = vi.fn()
    render(<SubmitPanel game={aGame()} onPlayCards={onPlayCards} onWriteAnswers={vi.fn()} />)

    const send = screen.getByRole('button', { name: /Choisissez 1 réponse/i })
    expect(send).toBeDisabled()

    await userEvent.click(screen.getByRole('button', { name: 'un chat mouillé' }))

    const ready = screen.getByRole('button', { name: /Envoyer ma réponse/i })
    expect(ready).toBeEnabled()
    await userEvent.click(ready)
    expect(onPlayCards).toHaveBeenCalledWith(['p1'], [[]])
  })

  it('drops the oldest pick once the limit is reached', async () => {
    const onPlayCards = vi.fn()
    render(<SubmitPanel game={aGame()} onPlayCards={onPlayCards} onWriteAnswers={vi.fn()} />)

    await userEvent.click(screen.getByRole('button', { name: 'un chat mouillé' }))
    await userEvent.click(screen.getByRole('button', { name: 'la honte de ma vie' }))
    await userEvent.click(screen.getByRole('button', { name: /Envoyer ma réponse/i }))

    expect(onPlayCards).toHaveBeenCalledWith(['p2'], [[]])
  })

  it('asks the player to complete a card that carries its own hole', async () => {
    const onPlayCards = vi.fn()
    const base = aGame()
    const game = aGame({
      you: {
        ...base.you,
        hand: [{ id: 'h1', text: "J'ai peur de ____", custom: false, blankCount: 1 }],
      },
    })
    render(<SubmitPanel game={game} onPlayCards={onPlayCards} onWriteAnswers={vi.fn()} />)

    await userEvent.click(screen.getByRole('button', { name: /J'ai peur de/ }))

    // Picked, but the hole is still empty: the send button stays locked.
    expect(screen.getByRole('button', { name: /Complétez les trous/i })).toBeDisabled()

    await userEvent.type(screen.getByLabelText(/trou 1/i), 'la lune')
    const ready = screen.getByRole('button', { name: /Envoyer ma réponse/i })
    expect(ready).toBeEnabled()
    await userEvent.click(ready)

    expect(onPlayCards).toHaveBeenCalledWith(['h1'], [['la lune']])
  })

  it('offers a card to write on in free mode', async () => {
    const onWriteAnswers = vi.fn()
    const game = aGame({
      settings: { ...aGame().settings, answerMode: 'FREE_TEXT' },
      you: { ...aGame().you, hand: [] },
    })
    render(<SubmitPanel game={game} onPlayCards={vi.fn()} onWriteAnswers={onWriteAnswers} />)

    await userEvent.type(screen.getByLabelText('Réponse 1'), 'une réponse maison')
    await userEvent.click(screen.getByRole('button', { name: /Envoyer ma réponse/i }))

    expect(onWriteAnswers).toHaveBeenCalledWith(['une réponse maison'])
  })

  it('tells the card czar they are not playing this round', () => {
    const game = aGame({ you: { ...aGame().you, isCzar: true, mustAnswer: false } })
    render(<SubmitPanel game={game} onPlayCards={vi.fn()} onWriteAnswers={vi.fn()} />)

    expect(screen.getByText(/Vous tranchez cette manche/i)).toBeInTheDocument()
  })

  it('confirms once the answer is gone', () => {
    const game = aGame({ you: { ...aGame().you, mustAnswer: false } })
    render(<SubmitPanel game={game} onPlayCards={vi.fn()} onWriteAnswers={vi.fn()} />)

    expect(screen.getByText(/Réponse envoyée/i)).toBeInTheDocument()
  })

  it('plays a random card on its own when the timer expires on an untouched hand', () => {
    vi.useFakeTimers()
    const onPlayCards = vi.fn()
    const game = aGame({ deadlineMillis: Date.now() + 5000, serverTimeMillis: Date.now() })
    render(<SubmitPanel game={game} onPlayCards={onPlayCards} onWriteAnswers={vi.fn()} />)

    act(() => vi.advanceTimersByTime(4000))

    expect(onPlayCards).toHaveBeenCalledTimes(1)
    const played = onPlayCards.mock.calls[0][0] as string[]
    expect(game.you.hand.map((card) => card.id)).toContain(played[0])
  })

  it('sends the selection already made rather than a random card', () => {
    vi.useFakeTimers()
    const onPlayCards = vi.fn()
    const game = aGame({ deadlineMillis: Date.now() + 5000, serverTimeMillis: Date.now() })
    render(<SubmitPanel game={game} onPlayCards={onPlayCards} onWriteAnswers={vi.fn()} />)

    // fireEvent rather than userEvent: the latter waits on timers this test controls.
    fireEvent.click(screen.getByRole('button', { name: 'la honte de ma vie' }))
    act(() => vi.advanceTimersByTime(4000))

    expect(onPlayCards).toHaveBeenCalledWith(['p2'], [[]])
  })

  it('never sends twice for the same round', () => {
    vi.useFakeTimers()
    const onPlayCards = vi.fn()
    const game = aGame({ deadlineMillis: Date.now() + 5000, serverTimeMillis: Date.now() })
    render(<SubmitPanel game={game} onPlayCards={onPlayCards} onWriteAnswers={vi.fn()} />)

    act(() => vi.advanceTimersByTime(4000))
    act(() => vi.advanceTimersByTime(2000))

    expect(onPlayCards).toHaveBeenCalledTimes(1)
  })

  it('stays quiet when the answer has already left', () => {
    vi.useFakeTimers()
    const onPlayCards = vi.fn()
    const base = aGame({ deadlineMillis: Date.now() + 5000, serverTimeMillis: Date.now() })
    const game = { ...base, you: { ...base.you, mustAnswer: false } }
    render(<SubmitPanel game={game} onPlayCards={onPlayCards} onWriteAnswers={vi.fn()} />)

    act(() => vi.advanceTimersByTime(4000))

    expect(onPlayCards).not.toHaveBeenCalled()
  })
})
