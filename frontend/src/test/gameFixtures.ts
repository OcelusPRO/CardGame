import type { GameView, PlayerView, PunchlineCardView } from '../api/types'

/** A believable table, so a component test only spells out what it asserts. */
export function aGame(overrides: Partial<GameView> = {}): GameView {
  return {
    code: 'ABCDE',
    phase: 'SUBMITTING',
    hostId: 'alice',
    settings: {
      selectionMode: 'VOTE',
      answerMode: 'CARDS',
      rounds: 8,
      handSize: 10,
      submitSeconds: 90,
      selectSeconds: 60,
      resultSeconds: 10,
      minPlayers: 3,
      maxPlayers: 12,
      allowSelfVote: false,
      czarAnswers: false,
      pointsPerVote: 1,
      unanimityBonus: 3,
      czarWinPoints: 3,
      twitchChatVote: false,
      twitchGuestChats: false,
    },
    players: [aPlayer('alice', 'Alice'), aPlayer('bob', 'Bob')],
    you: {
      id: 'alice',
      hand: [aCard('p1', 'un chat mouillé'), aCard('p2', 'la honte de ma vie')],
      isHost: true,
      isCzar: false,
      mustAnswer: true,
      mustVote: false,
    },
    round: {
      number: 1,
      situation: { id: 's1', text: 'Le pire, c\'est ____.', blankCount: 1, custom: false },
      expectedAnswers: 1,
      answers: [],
    },
    deck: { situationsLeft: 12, punchlinesLeft: 80 },
    serverTimeMillis: 1_000_000,
    chatChannels: [],
    ...overrides,
  }
}

export function aPlayer(id: string, nickname: string, overrides: Partial<PlayerView> = {}): PlayerView {
  return {
    id,
    nickname,
    avatar: {
      top: { styleId: 'head-round', color: '#ffd23f' },
      bottom: { styleId: 'body-tee', color: '#ff2e88' },
    },
    connected: true,
    score: 0,
    isHost: id === 'alice',
    isCzar: false,
    hasAnswered: false,
    hasVoted: false,
    ...overrides,
  }
}

export function aCard(id: string, text: string): PunchlineCardView {
  return { id, text, custom: false, blankCount: (text.match(/_{2,}/g) ?? []).length }
}
