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
    <div className="space-y-8">
      <header>
        <p className="text-xs font-medium uppercase tracking-widest text-accent">Directory</p>
        <h1 className="font-serif text-3xl font-semibold tracking-tight text-ink">{title}</h1>
      </header>

      <section className="rounded-lg border border-line bg-white p-6 shadow-sm">
        <h2 className="mb-4 text-sm font-semibold text-ink">
          {editId != null ? `Edit record #${editId}` : 'New record'}
        </h2>
        <form onSubmit={onSubmit} className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {fields.map((f) => (
              <label key={f.key} className="block text-sm font-medium text-muted">
                {f.label}
                <input
                  className={inputClass}
                  value={form[f.key] ?? ''}
                  required={f.required !== false}
                  onChange={(e) => setForm({ ...form, [f.key]: e.target.value })}
                />
              </label>
            ))}
          </div>
          {error && (
            <p className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800" role="alert">
              {error}
            </p>
          )}
          <div className="flex flex-wrap gap-3 pt-2">
            <button type="submit" className={btnPrimary}>
              {editId != null ? 'Save changes' : 'Add record'}
            </button>
            {editId != null && (
              <button type="button" className={btnSecondary} onClick={cancelEdit}>
                Cancel
              </button>
            )}
          </div>
        </form>
      </section>

      <section className="overflow-hidden rounded-lg border border-line bg-white shadow-sm">
        {loading ? (
          <p className="px-6 py-10 text-sm text-muted">Loading records…</p>
        ) : rows.length === 0 ? (
          <p className="px-6 py-10 text-sm text-muted">No records yet.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead className="border-b border-line bg-stone-50/80">
                <tr>
                  <th className="px-4 py-3 font-semibold text-muted">ID</th>
                  {fields.map((f) => (
                    <th key={f.key} className="px-4 py-3 font-semibold text-muted">
                      {f.label}
                    </th>
                  ))}
                  <th className="px-4 py-3 text-right font-semibold text-muted">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-line">
                {rows.map((row) => (
                  <tr key={row.id} className="hover:bg-stone-50/60">
                    <td className="px-4 py-3 tabular-nums text-muted">{row.id}</td>
                    {fields.map((f) => (
                      <td key={f.key} className="px-4 py-3 text-ink">
                        {String(row[f.key] ?? '') || '—'}
                      </td>
                    ))}
                    <td className="px-4 py-3 text-right whitespace-nowrap">
                      <button
                        type="button"
                        className="mr-3 text-sm font-medium text-brand hover:underline"
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
