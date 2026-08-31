import { api } from './client'
import type { AnswerMode, CardPackView, MeView } from './types'

/** Who the browser is, and which packs it may play with. */
export const sessionApi = {
  me: () => api.get<MeView>('/api/me'),
  logout: () => api.post<void>('/api/logout'),
  packs: (answerMode?: AnswerMode) =>
    api.get<CardPackView[]>(`/api/packs${answerMode ? `?answerMode=${answerMode}` : ''}`),
}
