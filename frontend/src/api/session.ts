import { api } from './client'
import type { AnswerMode, CardPackView, MeView } from './types'

/** Who the browser is, and which packs it may play with. */
export const sessionApi = {
  me: () => api.get<MeView>('/api/me'),
  logout: () => api.post<void>('/api/logout'),
  /**
   * The packs on offer. Pass a game `code` from a guest's lobby to get the paquet the
   * host actually built (so a guest never sees a pack the host has no access to) instead
   * of this browser's own catalogue.
   */
  packs: (answerMode?: AnswerMode, code?: string) => {
    const query = new URLSearchParams()
    if (answerMode) query.set('answerMode', answerMode)
    if (code) query.set('code', code)
    const suffix = query.toString()
    return api.get<CardPackView[]>(`/api/packs${suffix ? `?${suffix}` : ''}`)
  },
}
