import { useState, type FormEvent } from 'react'
import { Navigate, NavLink, Outlet, Route, Routes } from 'react-router-dom'
import { useAuth } from './auth'
import type { Role } from './api'
import { CrudPanel } from './CrudPanel'
import { btnPrimary, btnSecondary, inputClass } from './ui'

const personFields = [
  { key: 'firstName', label: 'First name' },
  { key: 'lastName', label: 'Last name' },
  { key: 'email', label: 'Email' },
]

const nav: { to: string; label: string; roles: Role[] }[] = [
  { to: '/admins', label: 'Administrators', roles: ['ROLE_ADMIN'] },
  { to: '/teachers', label: 'Faculty', roles: ['ROLE_ADMIN', 'ROLE_TEACHER'] },
  { to: '/students', label: 'Students', roles: ['ROLE_ADMIN', 'ROLE_STUDENT'] },
]

function roleName(role: Role) {
  return role.replace('ROLE_', '').toLowerCase()
}

function LoadingScreen() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-surface">
      <p className="text-sm text-muted">Loading…</p>
    </div>
  )
}

function Login() {
  const { user, loading, login } = useAuth()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  if (loading) return <LoadingScreen />
  if (user) return <Navigate to="/" replace />

  async function submit(e: FormEvent) {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      await login(username, password)
    } catch {
      setError('Invalid username or password.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="min-h-screen lg:grid lg:grid-cols-2">
      <div className="flex flex-col justify-between bg-brand px-8 py-10 text-white lg:px-14 lg:py-16">
        <div>
          <p className="text-xs font-medium uppercase tracking-[0.2em] text-white/70">Internal system</p>
          <h1 className="mt-4 font-serif text-4xl font-semibold leading-tight lg:text-5xl">
            University
            <span className="block text-white/90">Records office</span>
          </h1>
        </div>
        <p className="max-w-md text-sm leading-relaxed text-white/75">
          Manage administrators, faculty, and student directory entries. Access is limited by your assigned role.
        </p>
      </div>

      <div className="flex items-center justify-center px-6 py-12 lg:px-16">
        <div className="w-full max-w-sm">
          <h2 className="font-serif text-2xl font-semibold text-ink">Sign in</h2>
          <p className="mt-2 text-sm text-muted">Use your university account credentials.</p>

          <form onSubmit={submit} className="mt-8 space-y-5">
            <label className="block text-sm font-medium text-muted">
              Username
              <input
                className={inputClass}
                autoComplete="username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
              />
            </label>
            <label className="block text-sm font-medium text-muted">
              Password
              <input
                type="password"
                className={inputClass}
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </label>
            {error && (
              <p className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800" role="alert">
                {error}
              </p>
            )}
            <button type="submit" className={`${btnPrimary} w-full`} disabled={busy}>
              {busy ? 'Signing in…' : 'Sign in'}
            </button>
          </form>

          <p className="mt-8 border-t border-line pt-6 text-xs leading-relaxed text-muted">
            Demo: admin / admin123 · teacher / teacher123 · student / student123
          </p>
        </div>
      </div>
    </div>
  )
}

function RequireAuth() {
  const { user, loading, logout } = useAuth()
  if (loading) return <LoadingScreen />
  if (!user) return <Navigate to="/login" replace />

  const links = nav.filter((item) => item.roles.includes(user.role))

  return (
    <div className="flex min-h-screen bg-surface">
      <aside className="flex w-60 shrink-0 flex-col border-r border-line bg-white">
        <div className="border-b border-line px-5 py-6">
          <p className="text-xs font-medium uppercase tracking-widest text-accent">University</p>
          <p className="font-serif text-lg font-semibold text-ink">Records</p>
        </div>
        <nav className="flex-1 space-y-1 p-3" aria-label="Sections">
          {links.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                [
                  'block rounded-md px-3 py-2.5 text-sm font-medium transition',
                  isActive
                    ? 'bg-brand/10 text-brand'
                    : 'text-muted hover:bg-stone-100 hover:text-ink',
                ].join(' ')
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="border-t border-line p-4">
          <p className="truncate text-sm font-medium text-ink">{user.username}</p>
          <p className="text-xs capitalize text-muted">{roleName(user.role)}</p>
          <button type="button" className={`${btnSecondary} mt-3 w-full`} onClick={logout}>
            Sign out
          </button>
        </div>
      </aside>

      <main className="flex-1 overflow-auto">
        <div className="mx-auto max-w-5xl px-6 py-10 lg:px-10">
          <Outlet />
        </div>
      </main>
    </div>
  )
}

function DefaultPage() {
  const { user } = useAuth()
  if (!user) return null
  const first = nav.find((item) => item.roles.includes(user.role))
  return <Navigate to={first?.to ?? '/login'} replace />
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route element={<RequireAuth />}>
        <Route path="/" element={<DefaultPage />} />
        <Route path="/admins" element={<CrudPanel title="Administrators" basePath="/api/admins" fields={personFields} />} />
        <Route
          path="/teachers"
          element={
            <CrudPanel
              title="Faculty"
              basePath="/api/teachers"
              fields={[...personFields, { key: 'department', label: 'Department', required: false }]}
            />
          }
        />
        <Route
          path="/students"
          element={
            <CrudPanel
              title="Students"
              basePath="/api/students"
              fields={[...personFields, { key: 'major', label: 'Major', required: false }]}
            />
          }
        />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
