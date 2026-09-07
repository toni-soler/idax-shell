import React, { useEffect, useMemo, useState } from "react";
import { shellApi } from "./api.js";

const demoSeed = {
  users: [
    { userId: "demo-admin", displayName: "Local Administrator", email: "admin@local.test", role: "owner", enabled: true },
    { userId: "demo-reviewer", displayName: "Demo Reviewer", email: "reviewer@local.test", role: "reviewer", enabled: true },
  ],
  roles: [
    { id: "owner", key: "owner", name: "Owner", description: "Full workspace administration", permissionCount: 12, enabled: true, systemRole: true },
    { id: "reviewer", key: "reviewer", name: "Reviewer", description: "Read and verification access", permissionCount: 4, enabled: true, systemRole: false },
  ],
  alerts: [
    { id: "demo-alert", name: "Pending proofs", resource: "ledger.proofs", field: "status", value: "PENDING", schedule: "0 0 9 * * *", enabled: true, status: "APPROVED" },
  ],
};

const definitions = {
  users: {
    id: "userId", columns: ["displayName", "email", "role", "enabled"],
    fields: [
      { key: "displayName", required: true }, { key: "email", type: "email", required: true },
      { key: "role", required: true }, { key: "password", type: "password" }, { key: "enabled", type: "checkbox" },
    ],
  },
  roles: {
    id: "id", columns: ["name", "key", "description", "permissionCount", "enabled"],
    fields: [
      { key: "name", required: true }, { key: "key", required: true }, { key: "description" },
      { key: "permissions", hint: "permissionsHint" }, { key: "enabled", type: "checkbox" },
    ],
  },
  alerts: {
    id: "id", columns: ["name", "resource", "field", "value", "schedule", "status", "enabled"],
    fields: [
      { key: "name", required: true }, { key: "resource", required: true }, { key: "field", required: true },
      { key: "value", required: true }, { key: "schedule", required: true }, { key: "enabled", type: "checkbox" },
    ],
  },
};

const normalize = (kind, value) => {
  const next = { ...value };
  if (kind === "roles" && typeof next.permissions === "string") next.permissions = next.permissions.split(",").map((item) => item.trim()).filter(Boolean);
  return next;
};

function Editor({ kind, initial, isEditing, t, onClose, onSave }) {
  const definition = definitions[kind];
  const [value, setValue] = useState(() => ({ enabled: true, ...initial, permissions: initial?.permissions?.join?.(", ") || initial?.permissions || "" }));
  return <div className="dialog-backdrop" role="presentation" onMouseDown={onClose}><form className="crud-dialog" onMouseDown={(event) => event.stopPropagation()} onSubmit={(event) => { event.preventDefault(); onSave(normalize(kind, value)); }}>
    <div className="dialog-heading"><div><p className="kicker">{t(`crud.${kind}`)}</p><h2>{t(isEditing ? "crud.edit" : "crud.create")}</h2></div><button type="button" className="icon-button" onClick={onClose}>×</button></div>
    <div className="form-grid">{definition.fields.map((field) => <label key={field.key} className={field.type === "checkbox" ? "check-field" : ""}>{field.type === "checkbox" ? <><input type="checkbox" checked={Boolean(value[field.key])} onChange={(e) => setValue({ ...value, [field.key]: e.target.checked })}/><span>{t(`field.${field.key}`)}</span></> : <><span>{t(`field.${field.key}`)}</span><input type={field.type || "text"} required={field.required} value={value[field.key] ?? ""} onChange={(e) => setValue({ ...value, [field.key]: e.target.value })}/>{field.hint && <small>{t(`field.${field.hint}`)}</small>}</>}</label>)}</div>
    <div className="dialog-actions"><button type="button" className="secondary" onClick={onClose}>{t("crud.cancel")}</button><button>{t("crud.save")}</button></div>
  </form></div>;
}

function useFilterBar({ kind, t, tenantId, demo, onCreateAlert }) {
  const columns = definitions[kind].columns.filter((key) => key !== "enabled");
  const storageKey = `idax.demo.filters.${tenantId}.${kind}`;
  const [field, setField] = useState(columns[0]); const [value, setValue] = useState("");
  const [saved, setSaved] = useState(() => demo ? JSON.parse(localStorage.getItem(storageKey) || "[]") : []);
  useEffect(() => { if (!demo) shellApi.savedFilters(tenantId, kind).then(setSaved).catch(() => setSaved([])); }, [demo, tenantId, kind]);
  const persist = async (next) => { setSaved(next); if (demo) localStorage.setItem(storageKey, JSON.stringify(next)); else await shellApi.saveFilters(tenantId, kind, next); };
  const save = async () => { if (!value.trim()) return; const name = window.prompt(t("filters.name"), `${t(`field.${field}`)}: ${value}`); if (name) await persist([...saved, { id: crypto.randomUUID(), name, field, value }]); };
  return { field, value, toolbar: <><div className="filter-bar"><select value={field} onChange={(e) => setField(e.target.value)}>{columns.map((key) => <option key={key} value={key}>{t(`field.${key}`)}</option>)}</select><input value={value} onChange={(e) => setValue(e.target.value)} placeholder={t("filters.placeholder")}/><button className="secondary" onClick={save} disabled={!value.trim()}>{t("filters.save")}</button><button className="secondary" onClick={() => onCreateAlert({ resource: kind, field, value })} disabled={!value.trim()}>{t("filters.alert")}</button></div><div className="saved-filters">{saved.map((item) => <span key={item.id}><button onClick={() => { setField(item.field); setValue(item.value); }}>{item.name}</button><button aria-label={t("crud.delete")} onClick={() => persist(saved.filter((candidate) => candidate.id !== item.id))}>×</button></span>)}</div></> };
}

export default function CrudWorkspace({ kind, tenantId, session, t, onNavigate, draft }) {
  const definition = definitions[kind]; const demo = Boolean(session.demo);
  const [rows, setRows] = useState(demo ? demoSeed[kind] : []); const [loading, setLoading] = useState(!demo);
  const [error, setError] = useState(""); const [editing, setEditing] = useState(null); const [creating, setCreating] = useState(null);
  const load = () => { if (demo) return; setLoading(true); shellApi[kind](tenantId).then(setRows).catch(() => setError(t("admin.coreRequired"))).finally(() => setLoading(false)); };
  useEffect(load, [kind, tenantId, demo]);
  useEffect(() => { if (kind === "alerts" && draft) setCreating(draft); }, [kind, draft]);
  const alertDraft = (filter) => { if (kind === "alerts") { setEditing(null); setCreating(filter); } else onNavigate("alerts", filter); };
  const filter = useFilterBar({ kind, t, tenantId, demo, onCreateAlert: alertDraft });
  const visible = useMemo(() => rows.filter((row) => !filter.value || String(row[filter.field] ?? "").toLocaleLowerCase().includes(filter.value.toLocaleLowerCase())), [rows, filter.field, filter.value]);
  const save = async (value) => { setError(""); try { if (demo) { const idKey = definition.id; const next = editing ? rows.map((row) => row[idKey] === editing[idKey] ? { ...row, ...value } : row) : [...rows, { ...value, [idKey]: crypto.randomUUID(), status: kind === "alerts" ? "DRAFT" : value.status }]; setRows(next); } else { const id = editing?.[definition.id]; const action = id ? `update${kind[0].toUpperCase()}${kind.slice(1, -1)}` : `create${kind[0].toUpperCase()}${kind.slice(1, -1)}`; const saved = await shellApi[action](tenantId, ...(id ? [id, value] : [value])); if (saved) setRows(id ? rows.map((row) => row[definition.id] === id ? saved : row) : [...rows, saved]); else load(); } setEditing(null); setCreating(false); } catch { setError(t("admin.coreRequired")); } };
  const remove = async (row) => { if (!window.confirm(t("crud.confirmDelete"))) return; if (demo) setRows(rows.filter((item) => item[definition.id] !== row[definition.id])); else { try { const action = `delete${kind[0].toUpperCase()}${kind.slice(1, -1)}`; await shellApi[action](tenantId, row[definition.id]); load(); } catch { setError(t("admin.coreRequired")); } } };
  return <section className="crud-workspace"><div className="crud-heading"><div><p className="lead">{t(`${kind}.subtitle`)}</p></div><button onClick={() => setCreating({})}>+ {t("crud.create")}</button></div>{filter.toolbar}{error && <p className="capability-notice">{error}</p>}{loading ? <p>{t("crud.loading")}</p> : <div className="crud-table-wrap"><table><thead><tr>{definition.columns.map((key) => <th key={key}>{t(`field.${key}`)}</th>)}<th>{t("crud.actions")}</th></tr></thead><tbody>{visible.map((row) => <tr key={row[definition.id]}>{definition.columns.map((key) => <td key={key}>{typeof row[key] === "boolean" ? <span className={row[key] ? "state-on" : "state-off"}>{t(row[key] ? "state.enabled" : "state.disabled")}</span> : row[key] ?? "—"}</td>)}<td className="row-actions"><button onClick={() => setEditing(row)}>{t("crud.edit")}</button><button disabled={Boolean(row.systemRole)} onClick={() => remove(row)}>{t("crud.delete")}</button></td></tr>)}</tbody></table>{visible.length === 0 && <p className="empty-row">{t("crud.empty")}</p>}</div>}{(creating || editing) && <Editor kind={kind} initial={editing || creating} isEditing={Boolean(editing)} t={t} onClose={() => { setCreating(null); setEditing(null); }} onSave={save}/>}</section>;
}
