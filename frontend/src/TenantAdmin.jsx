import React, { useEffect, useState } from "react";
import { shellApi } from "./api.js";

/** Platform-level tenant management - visible only to a superuser (gated in App.jsx's nav array
 * and enforced again server-side by TenantAdminController). Deliberately its own component, not
 * another `kind` inside CrudWorkspace: every CrudWorkspace action hangs off a specific tenantId
 * (`/tenants/{tenantId}/...`), but creating a tenant has no tenant context to hang off yet - it
 * lives under `/platform/tenants` instead. Reuses the same dialog/table CSS as CrudWorkspace for
 * a consistent look without forcing an awkward fit into that component's assumptions. */
export default function TenantAdmin({ t }) {
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [creating, setCreating] = useState(false);
  const [editing, setEditing] = useState(null);
  const load = () => { setLoading(true); shellApi.platformTenants().then(setRows).catch((e) => setError(e.message)).finally(() => setLoading(false)); };
  useEffect(load, []);

  const toggleEnabled = async (row) => {
    setError("");
    try { const updated = await shellApi.setTenantEnabled(row.id, !row.enabled); setRows(rows.map((r) => r.id === row.id ? updated : r)); }
    catch (e) { setError(e.message); }
  };
  const saveName = async (row, name) => {
    setError("");
    try { const updated = await shellApi.updateTenantName(row.id, name); setRows(rows.map((r) => r.id === row.id ? updated : r)); setEditing(null); }
    catch (e) { setError(e.message); }
  };
  const create = async (request) => {
    setError("");
    try { const created = await shellApi.createPlatformTenant(request); setRows([...rows, created]); setCreating(false); }
    catch (e) { setError(e.message); }
  };

  return <section className="crud-workspace">
    <div className="crud-heading"><div><p className="lead">{t("tenants.subtitle")}</p></div><button onClick={() => setCreating(true)}>+ {t("crud.create")}</button></div>
    {error && <div className="capability-notice" role="alert"><span>{error}</span><button type="button" aria-label={t("crud.cancel")} onClick={() => setError("")}>×</button></div>}
    {loading ? <p>{t("crud.loading")}</p> : <div className="crud-table-wrap"><table><thead><tr><th>{t("field.name")}</th><th>{t("field.key")}</th><th>{t("field.enabled")}</th><th>{t("crud.actions")}</th></tr></thead><tbody>
      {rows.map((row) => <tr key={row.id}>
        <td>{row.name}</td><td>{row.code}</td>
        <td><span className={row.enabled ? "state-on" : "state-off"}>{t(row.enabled ? "state.enabled" : "state.disabled")}</span></td>
        <td className="row-actions">
          <button onClick={() => setEditing(row)}>{t("crud.edit")}</button>
          <button onClick={() => toggleEnabled(row)}>{t(row.enabled ? "tenants.disable" : "tenants.enable")}</button>
        </td>
      </tr>)}
    </tbody></table>{rows.length === 0 && <p className="empty-row">{t("crud.empty")}</p>}</div>}
    {creating && <CreateDialog t={t} onClose={() => setCreating(false)} onSave={create}/>}
    {editing && <RenameDialog t={t} tenant={editing} onClose={() => setEditing(null)} onSave={(name) => saveName(editing, name)}/>}
  </section>;
}

function CreateDialog({ t, onClose, onSave }) {
  const [value, setValue] = useState({ tenantCode: "", tenantName: "", adminEmail: "", adminDisplayName: "", adminPassword: "" });
  const set = (key) => (event) => setValue({ ...value, [key]: event.target.value });
  return <div className="dialog-backdrop" role="presentation" onMouseDown={onClose}><form className="crud-dialog" onMouseDown={(event) => event.stopPropagation()} onSubmit={(event) => { event.preventDefault(); onSave(value); }}>
    <div className="dialog-heading"><div><p className="kicker">{t("nav.tenants")}</p><h2>{t("crud.create")}</h2></div><button type="button" className="icon-button" onClick={onClose}>×</button></div>
    <div className="form-grid">
      <label><span>{t("tenants.name")}</span><input required maxLength={120} value={value.tenantName} onChange={set("tenantName")}/><small>{t("tenants.nameHint")}</small></label>
      <label><span>{t("tenants.code")}</span><input required maxLength={32} value={value.tenantCode} onChange={set("tenantCode")}/><small>{t("tenants.codeHint")}</small></label>
      <label><span>{t("tenants.adminDisplayName")}</span><input required maxLength={120} value={value.adminDisplayName} onChange={set("adminDisplayName")}/></label>
      <label><span>{t("tenants.adminEmail")}</span><input required type="email" value={value.adminEmail} onChange={set("adminEmail")}/></label>
      <label><span>{t("tenants.adminPassword")}</span><input required type="password" minLength={8} value={value.adminPassword} onChange={set("adminPassword")}/><small>{t("tenants.adminPasswordHint")}</small></label>
    </div>
    <div className="dialog-actions"><button type="button" className="secondary" onClick={onClose}>{t("crud.cancel")}</button><button>{t("crud.save")}</button></div>
  </form></div>;
}

function RenameDialog({ t, tenant, onClose, onSave }) {
  const [name, setName] = useState(tenant.name);
  return <div className="dialog-backdrop" role="presentation" onMouseDown={onClose}><form className="crud-dialog" onMouseDown={(event) => event.stopPropagation()} onSubmit={(event) => { event.preventDefault(); onSave(name); }}>
    <div className="dialog-heading"><div><p className="kicker">{t("nav.tenants")}</p><h2>{t("crud.edit")}</h2></div><button type="button" className="icon-button" onClick={onClose}>×</button></div>
    <div className="form-grid"><label><span>{t("tenants.name")}</span><input required maxLength={120} value={name} onChange={(event) => setName(event.target.value)}/></label></div>
    <div className="dialog-actions"><button type="button" className="secondary" onClick={onClose}>{t("crud.cancel")}</button><button>{t("crud.save")}</button></div>
  </form></div>;
}
