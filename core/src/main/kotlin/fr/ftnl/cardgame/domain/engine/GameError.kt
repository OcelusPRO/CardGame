package fr.ftnl.cardgame.domain.engine

/**
 * Why a command was refused. The code is sent as-is to the client, which owns the
 * translated wording, so the domain never carries user facing strings.
 */
enum class GameError {
    UNKNOWN_PLAYER,
    NOT_THE_HOST,
    NOT_THE_CZAR,
    CZAR_CANNOT_ANSWER,
    WRONG_PHASE,
    GAME_FULL,
    GAME_ALREADY_STARTED,
    NICKNAME_TAKEN,
    NOT_ENOUGH_PLAYERS,
    EMPTY_DECK,
    NOT_ENOUGH_CARDS,
    ALREADY_SUBMITTED,
    ALREADY_VOTED,
    WRONG_ANSWER_COUNT,
    WRONG_BLANK_COUNT,
    CARD_NOT_IN_HAND,
    INVALID_ANSWER,
    UNKNOWN_SUBMISSION,
    CANNOT_VOTE_OWN_ANSWER,
    NOT_IN_THIS_DUEL,
    CANNOT_JUDGE_OWN_DUEL,
    CANNOT_KICK_SELF,
    CHAT_VOTE_CLOSED,
    CHAT_CARDS_CLOSED,
    CHAT_CARDS_FULL,
    ONLY_THE_CHAT_VOTES,
}
