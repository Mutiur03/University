import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { api } from './api'
import type { FieldDef } from './api'
import { btnDanger, btnPrimary, btnSecondary, inputClass } from './ui'

type Row = { id?: number; [key: string]: string | number | undefined }

export function CrudPanel({
  title,
  basePath,
  fields,
}: {
  title: string
  basePath: string
  fields: FieldDef[]
}) {
  const empty = () => Object.fromEntries(fields.map((f) => [f.key, '']))
  const [rows, setRows] = useState<Row[]>([])
  const [form, setForm] = useState(empty)
  const [editId, setEditId] = useState<number | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

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
    try {
      const body = form as Row
      if (editId != null) await api.update(basePath, editId, { ...body, id: editId })
      else await api.create(basePath, body)
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

      <section className="editor-panel" aria-labelledby="editor-heading">
        <div className="editor-heading">
          <h2 id="editor-heading">{editId != null ? `Edit record #${editId}` : 'File a new record'}</h2>
          <span>{editId != null ? 'Editing mode' : 'New entry'}</span>
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
          <div className="editor-actions">
            <button type="submit" className={btnPrimary}>
              {editId != null ? 'Save changes' : 'File record'}
            </button>
            {editId != null && (
              <button type="button" className={btnSecondary} onClick={cancelEdit}>
                Cancel
              </button>
            )}
          </div>
        </form>
      </section>

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
                  <th scope="col">Actions</th>
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
                    <td>
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
                      <button type="button" className={btnDanger} onClick={() => void onDelete(row.id!)}>
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  )
}
