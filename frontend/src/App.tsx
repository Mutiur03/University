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
  { to: '/students', label: 'Students', roles: ['ROLE_ADMIN', 'ROLE_TEACHER', 'ROLE_STUDENT'] },
]

function roleName(role: Role) {
  return role.replace('ROLE_', '').toLowerCase()
}

function LoadingScreen() {
  return (
    <div className="auth-layout">
      <div className="auth-form-wrap">
        <p className="data-label">Loading university records…</p>
      </div>
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
    <div className="auth-layout">
      <div className="auth-poster">
        <div>
          <p className="eyebrow">Registrar’s office / 2026</p>
          <h1 className="auth-title">The people<br />behind <em>the campus.</em></h1>
          <p className="auth-copy">
            A quiet place to keep the university directory accurate, current, and useful.
          </p>
        </div>
        <p className="data-label">Private directory · role-based access</p>
      </div>

      <div className="auth-form-wrap">
        <div className="auth-form">
          <h2>Welcome back.</h2>
          <p className="form-intro">Sign in with your university account.</p>

          <form onSubmit={submit} className="form-stack">
            <label className="form-label" htmlFor="username">
              Username
              <input
                id="username"
                className={inputClass}
                autoComplete="username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
              />
            </label>
            <label className="form-label" htmlFor="password">
              Password
              <input
                id="password"
                type="password"
                className={inputClass}
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </label>
            {error && (
              <p className="auth-error" role="alert">
                {error}
              </p>
            )}
            <button type="submit" className={`${btnPrimary} w-full`} disabled={busy}>
              {busy ? 'Signing in…' : 'Sign in'}
            </button>
          </form>

          <p className="demo-note">
            Demo administrator: admin / admin123
          </p>
        </div>
      </div>
    </div>
  )
}

function TeacherDirectory() {
  const { user } = useAuth()
  const isAdmin = user?.role === 'ROLE_ADMIN'
  return (
    <CrudPanel
      title="Faculty"
      basePath="/api/teachers"
      fields={[...personFields, { key: 'department', label: 'Department', required: false }]}
      canCreate={isAdmin}
      canDelete={isAdmin}
      canEdit={isAdmin}
    />
  )
}

function StudentDirectory() {
  const { user } = useAuth()
  const isAdmin = user?.role === 'ROLE_ADMIN'
  const isStudent = user?.role === 'ROLE_STUDENT'
  return (
    <CrudPanel
      title="Students"
      basePath="/api/students"
      fields={[...personFields, { key: 'major', label: 'Major', required: false }]}
      canCreate={isAdmin}
      canDelete={isAdmin}
      canEdit={isAdmin}
      ownProfile={isStudent}
    />
  )
}

function RequireAuth() {
  const { user, loading, logout } = useAuth()
  if (loading) return <LoadingScreen />
  if (!user) return <Navigate to="/login" replace />

  const links = nav.filter((item) => item.roles.includes(user.role))

  return (
    <div className="app-shell">
      <a href="#main-content" className="skip-link">Skip to content</a>
      <aside className="sidebar">
        <div className="sidebar-brand">
          <p className="brand-mark">University / Records</p>
          <p className="brand-name">Registrar’s desk</p>
        </div>
        <nav className="sidebar-nav" aria-label="Directory sections">
          <span className="nav-label data-label">Directories</span>
          {links.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `nav-link${isActive ? ' active' : ''}`
              }
            >
              <span className="nav-index">{String(links.indexOf(item) + 1).padStart(2, '0')}</span>
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-account">
          <div>
            <p className="account-name">{user.username}</p>
            <p className="account-role">{roleName(user.role)}</p>
          </div>
          <button type="button" className={`${btnSecondary} account-signout`} onClick={logout}>
            Sign out
          </button>
        </div>
      </aside>

      <main id="main-content" className="main-content">
        <div className="content-wrap">
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
            <TeacherDirectory />
          }
        />
        <Route
          path="/students"
          element={
            <StudentDirectory />
          }
        />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
