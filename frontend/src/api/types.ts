/**
 * Mirror of the payloads produced by the Ktor server. Keeping them in one file makes
 * a contract change impossible to miss: the compiler points at every screen.
 */

export type GamePhase = 'LOBBY' | 'SUBMITTING' | 'SELECTING' | 'ROUND_RESULT' | 'FINISHED'
export type SelectionMode = 'VOTE' | 'CZAR'
export type AnswerMode = 'CARDS' | 'FREE_TEXT'

export interface AvatarPartView {
  styleId: string
  color: string
}

export interface AvatarView {
  top: AvatarPartView
  bottom: AvatarPartView
  discordAvatarUrl?: string
}

export interface PlayerView {
  id: string
  nickname: string
  avatar: AvatarView
  connected: boolean
  score: number
  isHost: boolean
  isCzar: boolean
  hasAnswered: boolean
  hasVoted: boolean
}

export interface PunchlineCardView {
  id: string
  text: string
  custom: boolean
  /** Number of holes (`____`) the player fills in when playing this card. */
  blankCount: number
}

export interface SituationCardView {
  id: string
  text: string
  blankCount: number
  custom: boolean
}

export interface AnswerView {
  id: number
  texts: string[]
  filledText: string
  authorId?: string
  votes?: number
  isMine: boolean
}

export interface RoundOutcomeView {
  points: Record<string, number>
  winners: string[]
  /** The answer to put on stage at the reveal, and the one that earned any bonus. */
  topAnswerId?: number
}

export interface RoundView {
  number: number
  situation: SituationCardView
  expectedAnswers: number
  czarId?: string
  answers: AnswerView[]
  myVote?: number
  outcome?: RoundOutcomeView
}

export interface SelfView {
  id: string
  hand: PunchlineCardView[]
  isHost: boolean
  isCzar: boolean
  mustAnswer: boolean
  mustVote: boolean
}

export interface DeckSummary {
  situationsLeft: number
  punchlinesLeft: number
}

export interface GameSettingsView {
  selectionMode: SelectionMode
  answerMode: AnswerMode
  rounds: number
  handSize: number
  submitSeconds: number
  selectSeconds: number
  resultSeconds: number
  minPlayers: number
  maxPlayers: number
  allowSelfVote: boolean
  czarAnswers: boolean
  pointsPerVote: number
  unanimityBonus: number
  czarWinPoints: number
}

export interface GameView {
  code: string
  phase: GamePhase
  hostId: string
  settings: GameSettingsView
  players: PlayerView[]
  you: SelfView
  round?: RoundView
  deck: DeckSummary
  deadlineMillis?: number
  serverTimeMillis: number
}

export interface GameTicket {
  code: string
  playerId: string
  joinUrl: string
  isHost: boolean
}

export interface GamePreview {
  code: string
  phase: GamePhase
  hostNickname: string
  playerCount: number
  maxPlayers: number
  canJoin: boolean
  /** What lets a single address serve both the table and the form to join it. */
  youArePlaying: boolean
}

export interface MeView {
  playerId: string
  discordConnected: boolean
  discordUsername?: string
  discordAvatarUrl?: string
  isAdmin: boolean
  discordLoginAvailable: boolean
}

export interface CardPackView {
  id: string
  name: string
  description: string
  situationCount: number
  punchlineCount: number
  /** Marked "interdit aux mineurs". Only ever present for hosts cleared for it. */
  adultOnly: boolean
}

export interface AvatarInput {
  topStyleId: string
  topColor: string
  bottomStyleId: string
  bottomColor: string
}

export interface GameSettingsInput {
  selectionMode?: SelectionMode
  answerMode?: AnswerMode
  rounds?: number
  handSize?: number
  submitSeconds?: number
  selectSeconds?: number
  resultSeconds?: number
  minPlayers?: number
  maxPlayers?: number
  allowSelfVote?: boolean
  czarAnswers?: boolean
  pointsPerVote?: number
  unanimityBonus?: number
  czarWinPoints?: number
}

export interface DeckInput {
  packIds: string[]
  customSituations: string[]
  customPunchlines: string[]
}
