export type Role = 'ROLE_ADMIN' | 'ROLE_TEACHER' | 'ROLE_STUDENT'

export type AuthUser = { username: string; role: Role }

type EntityRecord = Record<string, unknown>

export type FieldDef = { key: string; label: string; required?: boolean }

const API = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'
const TOKEN_KEY = 'university_token'

function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(init.headers as Record<string, string> | undefined),
  }
  const token = getToken()
  if (token) headers.Authorization = `Bearer ${token}`

  const res = await fetch(`${API}${path}`, { ...init, headers })
  if (res.status === 204) return undefined as T
  if (!res.ok) {
    if (res.status === 401) clearToken()
    throw new Error(res.status === 403 ? 'Forbidden' : `HTTP ${res.status}`)
  }
  return res.json() as Promise<T>
}

export const api = {
  me: () => request<AuthUser>('/api/auth/me'),
  login: async (username: string, password: string) => {
    const data = await request<AuthUser & { accessToken: string }>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    })
    setToken(data.accessToken)
    return { username: data.username, role: data.role }
  },
  list: (path: string) => request<EntityRecord[]>(path),
  create: (path: string, body: Record<string, unknown>) =>
    request<EntityRecord>(path, { method: 'POST', body: JSON.stringify(body) }),
  update: (path: string, id: number, body: Record<string, unknown>) =>
    request<EntityRecord>(`${path}/${id}`, {
      method: 'PUT',
      body: JSON.stringify(body),
    }),
  remove: (path: string, id: number) =>
    request<void>(`${path}/${id}`, { method: 'DELETE' }),
}
