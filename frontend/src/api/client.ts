import { ApiError } from './ApiError'

/**
 * The single place that talks HTTP. Cookies ride along by default, which is how the
 * server recognises the player and the administrator.
 */
async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...(init?.headers ?? {}) },
  })
  if (!response.ok) throw await toError(response)
  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}

async function toError(response: Response): Promise<ApiError> {
  const body = await response.json().catch(() => null)
  const code = typeof body?.code === 'string' ? body.code : 'NETWORK_ERROR'
  return new ApiError(code, response.status, body?.detail)
}

export const api = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'POST', body: body === undefined ? undefined : JSON.stringify(body) }),
  remove: <T>(path: string) => request<T>(path, { method: 'DELETE' }),
}
