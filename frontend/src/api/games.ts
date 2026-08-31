import { api } from './client'
import type { AvatarInput, GamePreview, GameSettingsInput, GameTicket } from './types'

/** Everything the lobby screens need before the socket takes over. */
export const gamesApi = {
  create: (nickname: string, avatar: AvatarInput, settings?: GameSettingsInput) =>
    api.post<GameTicket>('/api/games', { nickname, avatar, settings }),

  join: (code: string, nickname: string, avatar: AvatarInput) =>
    api.post<GameTicket>(`/api/games/${code}/players`, { nickname, avatar }),

  preview: (code: string) => api.get<GamePreview>(`/api/games/${code}`),
}
