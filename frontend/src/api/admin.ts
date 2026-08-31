import { api } from './client'
import type {
  AdminOverview,
  CardAdminView,
  CardInput,
  CardStatsView,
  PackAdminView,
  ComboView,
  CardUsageView,
  DailyActivityView,
  PackInput,
} from './adminTypes'

/** The administration endpoints, all behind the Discord allowlist. */
export const adminApi = {
  overview: () => api.get<AdminOverview>('/api/admin/stats/overview'),
  activity: (days = 30) => api.get<DailyActivityView[]>(`/api/admin/stats/activity?days=${days}`),
  topCards: (kind: 'SITUATION' | 'PUNCHLINE', limit = 15) =>
    api.get<CardUsageView[]>(`/api/admin/stats/cards?kind=${kind}&limit=${limit}`),
  cardStats: (id: string) =>
    api.get<CardStatsView>(`/api/admin/stats/cards/${encodeURIComponent(id)}`),
  combos: (limit = 15, minPlays = 2) =>
    api.get<ComboView[]>(`/api/admin/stats/combos?limit=${limit}&minPlays=${minPlays}`),

  packs: () => api.get<PackAdminView[]>('/api/admin/packs'),
  savePack: (input: PackInput) => api.post<PackAdminView>('/api/admin/packs', input),
  deletePack: (id: string) => api.remove<void>(`/api/admin/packs/${id}`),

  situations: () => api.get<CardAdminView[]>('/api/admin/situations'),
  saveSituation: (input: CardInput) => api.post<CardAdminView>('/api/admin/situations', input),
  deleteSituation: (id: string) => api.remove<void>(`/api/admin/situations/${id}`),

  punchlines: () => api.get<CardAdminView[]>('/api/admin/punchlines'),
  savePunchline: (input: CardInput) => api.post<CardAdminView>('/api/admin/punchlines', input),
  deletePunchline: (id: string) => api.remove<void>(`/api/admin/punchlines/${id}`),
}
