import type { GameView } from '../api/types'

/**
 * Whether the viewer is allowed to pick their own answer.
 *
 * This is the client half of `GameState.allowsSelfVote` on the server, and it has to stay
 * the same answer: a card offered here that the server refuses turns into an error toast
 * on a table where nothing was actually wrong.
 *
 * The czar is the exception, and not an optional one. On a table where the judge also
 * plays, they are the only voter of the round — barring them from their own card would
 * mean the card they just played could never win.
 */
export function canVoteOwnAnswer(game: GameView): boolean {
  if (game.settings.selectionMode === 'CZAR') return game.settings.czarAnswers && game.you.isCzar
  return game.settings.allowSelfVote
}
