/**
 * Mirror of the payloads produced by the Ktor server. Keeping them in one file makes
 * a contract change impossible to miss: the compiler points at every screen.
 */

export type GamePhase = 'LOBBY' | 'SUBMITTING' | 'SELECTING' | 'ROUND_RESULT' | 'FINISHED'
/** Who designates the best answer. */
export type SelectionMode = 'VOTE' | 'CZAR' | 'CHAT'
/** How they do it — every answer at once, or two at a time. Free of the mode. */
export type SelectionFormat = 'ALL_AT_ONCE' | 'DUELS'
export type AnswerMode = 'CARDS' | 'FREE_TEXT'
/** What a viewer has to do before their idea is allowed onto the table. */
export type ChatCardAccess = 'OFF' | 'EVERYONE' | 'CHANNEL_POINTS' | 'BITS'

/**
 * One channel point reward for a pile. A blank `id` means the game creates it, under the
 * title and cost given here; a filled one names a reward already on the channel.
 */
export interface ChatCardRewardView {
  id: string
  title: string
  cost: number
}

/**
 * A reward already standing on the host's channel.
 *
 * `managed` is what the choice turns on: Twitch only lets the application that created a
 * reward fulfil or cancel its redemptions, so a reward built in the streamer's dashboard
 * can be read by the game but never answered for.
 */
export interface ChannelRewardView {
  id: string
  title: string
  cost: number
  managed: boolean
}

/** Whether — and at what price — the viewers may write cards into the game. */
export interface ChatCardsView {
  access: ChatCardAccess
  situations: boolean
  punchlines: boolean
  minBits: number
  situationReward: ChatCardRewardView
  punchlineReward: ChatCardRewardView
}

export interface ChatCardRewardInput {
  id?: string
  title?: string
  cost?: number
}

export interface ChatCardsInput {
  access?: ChatCardAccess
  situations?: boolean
  punchlines?: boolean
  minBits?: number
  situationReward?: ChatCardRewardInput
  punchlineReward?: ChatCardRewardInput
}

/** One card a viewer wrote, as the host reads it back in their own card boxes. */
export interface ChatWrittenCardView {
  id: string
  text: string
}

/** Everything the chats wrote into the paquet. Only ever filled for the host. */
export interface ChatCardLogView {
  situations: ChatWrittenCardView[]
  punchlines: ChatWrittenCardView[]
}

export interface AvatarPartView {
  styleId: string
  color: string
}

export interface AvatarView {
  top: AvatarPartView
  bottom: AvatarPartView
  /** The Discord or Twitch profile picture, drawn over the top half. */
  pictureUrl?: string
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
  /** Their Twitch channel, when they signed in with it. */
  twitchLogin?: string
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

/** One viewer who voted from a Twitch chat, as their own chat shows them. */
export interface ChatVoterView {
  id: string
  name: string
  avatarUrl?: string
}

/** What the Twitch chats gave an answer: a number of voices, and the first faces. */
export interface ChatVotesView {
  count: number
  voters: ChatVoterView[]
}

export interface AnswerView {
  id: number
  texts: string[]
  filledText: string
  authorId?: string
  votes?: number
  isMine: boolean
  /** Live while the chat judges: one voice per viewer, plus the faces to show. */
  chatVotes?: ChatVotesView
}

export interface RoundOutcomeView {
  points: Record<string, number>
  winners: string[]
  /** The answer to put on stage at the reveal, and the one that earned any bonus. */
  topAnswerId?: number
}

/** How many duels an answer has won so far in the knockout ladder. */
export interface DuelWinsView {
  answerId: number
  wins: number
}

/**
 * The knockout ladder of a round, in the `DUELS` format: which two answers are facing off
 * right now, how far along the tier is, and what each answer has already won.
 */
export interface BracketView {
  tier: number
  duelNumber: number
  duelCount: number
  left?: number
  right?: number
  wins: DuelWinsView[]
  championId?: number
}

export interface RoundView {
  number: number
  situation: SituationCardView
  expectedAnswers: number
  czarId?: string
  answers: AnswerView[]
  myVote?: number
  bracket?: BracketView
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
  selectionFormat: SelectionFormat
  answerMode: AnswerMode
  rounds: number
  handSize: number
  submitSeconds: number
  selectSeconds: number
  /** How long a single duel lasts in the `DUELS` format; ignored when judging all at once. */
  duelSeconds: number
  resultSeconds: number
  minPlayers: number
  maxPlayers: number
  allowSelfVote: boolean
  czarAnswers: boolean
  pointsPerVote: number
  unanimityBonus: number
  /** In the `CHAT` mode, the chats of the other streamers at the table are read too. */
  twitchGuestChats: boolean
  chatCards: ChatCardsView
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
  /** The Twitch channels whose chat votes on this table; empty when nobody's does. */
  chatChannels: string[]
  /** What the chats wrote into the paquet, for the host to keep or refuse. */
  chatCardLog: ChatCardLogView
  /**
   * True when the server projected this for the stream page rather than for a seat: `you`
   * is empty and nothing on screen may act. Absent on a player's snapshot.
   */
  spectator?: boolean
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
  twitchConnected: boolean
  twitchUsername?: string
  twitchAvatarUrl?: string
  /** The channel name, which is what the table reads a chat from. */
  twitchLogin?: string
  isAdmin: boolean
  discordLoginAvailable: boolean
  twitchLoginAvailable: boolean
  /** Whether this server has a Twitch extension at all; without one there are no bits. */
  twitchExtensionAvailable: boolean
  /** Whether this Twitch account let the game own channel point rewards on its channel. */
  twitchRewardsAuthorized: boolean
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
  selectionFormat?: SelectionFormat
  answerMode?: AnswerMode
  rounds?: number
  handSize?: number
  submitSeconds?: number
  selectSeconds?: number
  duelSeconds?: number
  resultSeconds?: number
  minPlayers?: number
  maxPlayers?: number
  allowSelfVote?: boolean
  czarAnswers?: boolean
  pointsPerVote?: number
  unanimityBonus?: number
  twitchGuestChats?: boolean
  chatCards?: ChatCardsInput
}

export interface DeckInput {
  packIds: string[]
  customSituations: string[]
  customPunchlines: string[]
}
