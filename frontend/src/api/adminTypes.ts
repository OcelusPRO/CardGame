/** Payloads used by the administration screens only. */

export interface PackAdminView {
  id: string
  name: string
  description: string
  enabled: boolean
  answerModeCards: boolean
  answerModeFreeText: boolean
  adultOnly: boolean
  situationCount: number
  punchlineCount: number
}

export interface CardAdminView {
  id: string
  packId: string
  text: string
  enabled: boolean
  blankCount?: number
}

export interface CardInput {
  id?: string
  packId: string
  text: string
  enabled: boolean
}

export interface PackInput {
  id?: string
  name: string
  description: string
  enabled: boolean
  answerModeCards?: boolean
  answerModeFreeText?: boolean
  adultOnly?: boolean
}

export interface AdultAccessView {
  discordId: string
  label: string
  addedAtMillis: number
}

export interface AdultAccessInput {
  discordId: string
  label?: string
}

export interface DeckImportInput {
  packId?: string
  name: string
  description: string
  answerModeCards: boolean
  answerModeFreeText: boolean
  situations: string[]
  punchlines: string[]
}

export interface LiveStatsView {
  activeGames: number
  connectedPlayers: number
  timestampMillis: number
}

export interface DailyActivityView {
  day: string
  gamesCreated: number
  roundsPlayed: number
  answersPlayed: number
}

export interface AdminOverview {
  packs: number
  situations: number
  punchlines: number
  live: LiveStatsView
  today: DailyActivityView
}

export interface CardUsageView {
  cardId: string
  kind: 'SITUATION' | 'PUNCHLINE'
  text: string
  deals: number
  plays: number
  votes: number
  wins: number
}

export interface BestSituationView {
  situationId: string
  text: string
  plays: number
  votes: number
  wins: number
}

export interface CardStatsView {
  cardId: string
  text: string
  deals: number
  plays: number
  votes: number
  wins: number
  bestSituation: BestSituationView | null
}

export interface ComboView {
  situationId: string
  situationText: string
  punchlineId: string
  punchlineText: string
  plays: number
  votes: number
  wins: number
  voteRatio: number
}
