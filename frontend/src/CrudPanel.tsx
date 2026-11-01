import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { api } from './api'
import type { FieldDef } from './api'
import { btnDanger, btnPrimary, btnSecondary, inputClass } from './ui'

type Row = { id?: number; [key: string]: string | number | undefined }

export function CrudPanel({
  title,
  basePath,
  fields,
  canCreate = true,
  canDelete = true,
  canEdit = true,
  ownProfile = false,
}: {
  title: string
  basePath: string
  fields: FieldDef[]
  canCreate?: boolean
  canDelete?: boolean
  canEdit?: boolean
  ownProfile?: boolean
}) {
  const empty = () => Object.fromEntries(fields.map((f) => [f.key, '']))
  const [rows, setRows] = useState<Row[]>([])
  const [form, setForm] = useState(empty)
  const [editId, setEditId] = useState<number | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [credentials, setCredentials] = useState<{ username: string; temporaryPassword: string } | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      setRows((await api.list(basePath)) as unknown as Row[])
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Load failed')
    } finally {
      setLoading(false)
    }
  }, [basePath])

  useEffect(() => {
    void load()
  }, [load])

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setCredentials(null)
    try {
      const body = form as Row
      if (editId != null) {
        await api.update(basePath, editId, { ...body, id: editId })
      } else {
        const created = await api.create(basePath, body)
        const account = created.credentials as
          | { username: string; temporaryPassword: string }
          | undefined
        if (account) setCredentials(account)
      }
      setEditId(null)
      setForm(empty())
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Save failed')
    }
  }

  async function onDelete(id: number) {
    if (!confirm('Delete this record?')) return
    try {
      await api.remove(basePath, id)
      if (editId === id) {
        setEditId(null)
        setForm(empty())
      }
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Delete failed')
    }
  }

  function cancelEdit() {
    setEditId(null)
    setForm(empty())
    setError(null)
  }

  return (
    <div>
      <header className="page-header">
        <div>
          <p className="eyebrow">Directory / {basePath.split('/').pop()}</p>
          <h1 className="page-title">{title}</h1>
          <p className="page-subtitle">
            Keep this part of the university directory clear, current, and easy to trust.
          </p>
        </div>
        <div className="record-count" aria-label={`${rows.length} records`}>
          <strong>{String(rows.length).padStart(2, '0')}</strong>
          <span>records filed</span>
        </div>
      </header>

      {(canCreate || canEdit) && (
      <section className="editor-panel" aria-labelledby="editor-heading">
        <div className="editor-heading">
          <h2 id="editor-heading">{editId != null ? `Edit record #${editId}` : canCreate ? 'File a new record' : 'Your faculty record'}</h2>
          <span>{editId != null ? 'Editing mode' : canCreate ? 'New entry' : 'Personal details'}</span>
        </div>
        <form onSubmit={onSubmit}>
          <div className="field-grid">
            {fields.map((f) => (
              <label key={f.key} className="form-label" htmlFor={`field-${f.key}`}>
                {f.label}
                <input
                  id={`field-${f.key}`}
                  className={inputClass}
                  value={form[f.key] ?? ''}
                  required={f.required !== false}
                  readOnly={!canEdit}
                  onChange={(e) => setForm({ ...form, [f.key]: e.target.value })}
                />
              </label>
            ))}
          </div>
          {error && (
            <p className="panel-error" role="alert">
              {error}
            </p>
          )}
          {credentials && (
            <div className="credentials-note" role="status">
              <strong>Login created</strong>
              <span>Username: {credentials.username}</span>
              <span>Temporary password: {credentials.temporaryPassword}</span>
              <small>Save this password now. It will not be shown again.</small>
            </div>
          )}
          <div className="editor-actions">
            {(editId != null || canCreate) && canEdit && (
              <button type="submit" className={btnPrimary}>
                {editId != null ? 'Save changes' : 'File record'}
              </button>
            )}
            {editId != null && (
              <button type="button" className={btnSecondary} onClick={cancelEdit}>
                Cancel
              </button>
            )}
          </div>
        </form>
      </section>
      )}

      {ownProfile ? (
        <section className="profile-panel" aria-label={`${title} profile`}>
          {loading ? (
            <p className="empty-state">Loading profile…</p>
          ) : rows.length === 0 ? (
            <p className="empty-state">No record on file.</p>
          ) : (
            (() => {
              const row = rows[0]
              const initial = String(row[fields[0]?.key] ?? '?').charAt(0).toUpperCase()
              return (
                <>
                  <div className="profile-identity">
                    <div className="profile-avatar">{initial}</div>
                    <div>
                      <h2>{fields.slice(0, 2).map((f) => row[f.key]).filter(Boolean).join(' ') || title}</h2>
                      <span>Record #{row.id}</span>
                    </div>
                  </div>
                  <dl className="profile-grid">
                    {fields.map((f) => (
                      <div key={f.key} className="profile-field">
                        <dt>{f.label}</dt>
                        <dd>{String(row[f.key] ?? '') || '—'}</dd>
                      </div>
                    ))}
                  </dl>
                </>
              )
            })()
          )}
        </section>
      ) : (
      <section className="table-panel" aria-label={`${title} records`}>
        {loading ? (
          <p className="empty-state">Loading records…</p>
        ) : rows.length === 0 ? (
          <p className="empty-state">No records yet. File the first entry above.</p>
        ) : (
          <div className="table-scroll">
            <table className="records-table">
              <thead>
                <tr>
                  <th scope="col">ID</th>
                  {fields.map((f) => (
                    <th key={f.key} scope="col">
                      {f.label}
                    </th>
                  ))}
                  {(canEdit || canDelete) && <th scope="col">Actions</th>}
                </tr>
              </thead>
              <tbody>
                {rows.map((row) => (
                  <tr key={row.id} className="hover:bg-stone-50/60">
                    <td className="record-id">{row.id}</td>
                    {fields.map((f) => (
                      <td key={f.key}>
                        {String(row[f.key] ?? '') || '—'}
                      </td>
                    ))}
                    {(canEdit || canDelete) && (
                    <td>
                      {canEdit && (
                        <button
                          type="button"
                          className="button button-secondary"
                          style={{ marginRight: '.5rem', minHeight: '2rem', padding: '.35rem .6rem', fontSize: '.68rem' }}
                          onClick={() => {
                            setEditId(row.id!)
                            setForm(
                              Object.fromEntries(fields.map((f) => [f.key, String(row[f.key] ?? '')])),
                            )
                          }}
                        >
                          Edit
                        </button>
                      )}
                      {canDelete && (
                        <button type="button" className={btnDanger} onClick={() => void onDelete(row.id!)}>
                          Delete
                        </button>
                      )}
                    </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
      )}
    </div>
  )
}
