/**
 * The server only ever sends a code, so the wording lives here, in one place, in French.
 */
const MESSAGES: Record<string, string> = {
  UNKNOWN_PLAYER: "On ne vous trouve pas à cette table.",
  NOT_THE_HOST: "Seul l'hôte peut faire ça.",
  NOT_THE_CZAR: "Cette manche, ce n'est pas vous qui tranchez.",
  CZAR_CANNOT_ANSWER: "Le maître du jeu ne joue pas cette manche.",
  WRONG_PHASE: "Ce n'est pas le moment.",
  GAME_FULL: "La table est complète.",
  GAME_ALREADY_STARTED: "La partie a déjà commencé.",
  NICKNAME_TAKEN: "Ce pseudo est déjà pris.",
  NOT_ENOUGH_PLAYERS: "Il manque encore du monde.",
  EMPTY_DECK: "Aucune carte situation dans ce paquet.",
  NOT_ENOUGH_CARDS: "Pas assez de cartes réponses pour distribuer.",
  ALREADY_SUBMITTED: "Vous avez déjà joué cette manche.",
  ALREADY_VOTED: "Vous avez déjà voté.",
  WRONG_ANSWER_COUNT: "Le nombre de réponses ne colle pas à la situation.",
  WRONG_BLANK_COUNT: "Il reste un trou à compléter sur une de vos cartes.",
  CARD_NOT_IN_HAND: "Cette carte n'est pas dans votre main.",
  INVALID_ANSWER: "Cette réponse ne passe pas (vide ou trop longue).",
  UNKNOWN_SUBMISSION: "Cette réponse n'existe plus.",
  CANNOT_VOTE_OWN_ANSWER: "On ne vote pas pour soi, ce serait trop facile.",
  CANNOT_KICK_SELF: "Vous ne pouvez pas vous exclure vous-même.",
  GAME_NOT_FOUND: "Cette partie n'existe pas ou est terminée.",
  INVALID_GAME_CODE: "Ce code de partie est invalide.",
  NICKNAME_REQUIRED: "Choisissez un pseudo.",
  VALIDATION_ERROR: "Une valeur saisie n'est pas valide.",
  MALFORMED_REQUEST: "La requête est mal formée.",
  ADMIN_REQUIRED: "Réservé aux administrateurs.",
  PACK_NOT_EMPTY: "Ce pack contient encore des cartes.",
  PACK_NOT_FOUND: "Ce pack n'existe pas.",
  CARD_NOT_FOUND: "Cette carte n'existe pas.",
  BAD_MESSAGE: "Message incompris par le serveur.",
  INTERNAL_ERROR: "Le serveur a fait une bêtise.",
  NETWORK_ERROR: "Connexion impossible.",
}

export function errorMessage(code: string | null | undefined): string {
  if (!code) return ''
  return MESSAGES[code] ?? "Quelque chose s'est mal passé."
}
