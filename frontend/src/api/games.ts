import { api } from './client'
import type { AvatarInput, GamePreview, GameSettingsInput, GameTicket } from './types'

/** Everything the lobby screens need before the socket takes over. */
export const gamesApi = {
  /** `sharedDevice` opens a table the whole room plays on this one device. */
  create: (nickname: string, avatar: AvatarInput, settings?: GameSettingsInput, sharedDevice = false) =>
    api.post<GameTicket>('/api/games', { nickname, avatar, settings, sharedDevice }),

  join: (code: string, nickname: string, avatar: AvatarInput) =>
    api.post<GameTicket>(`/api/games/${code}/players`, { nickname, avatar }),

  preview: (code: string) => api.get<GamePreview>(`/api/games/${code}`),
}
