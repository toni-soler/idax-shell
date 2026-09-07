import React, { useEffect, useMemo, useState } from "react";
import { shellApi } from "./api.js";
import { localeNames, resolveLocale, translate } from "./i18n.js";
import { useLocation, useNavigate } from "react-router-dom";
import ModuleHost from "./ModuleHost.jsx";
import CrudWorkspace from "./CrudWorkspace.jsx";

const demoModules = [
  { id: "ledger", glyph: "L", color: "#65e6cc", route: "/ledger" },
  { id: "ostris", glyph: "O", color: "#f5b65b", route: "/ostris" },
];

function Login({ t, locale, setLocale, onEnter }) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const submit = async (event) => {
    event.preventDefault(); setError("");
    try { const session = await shellApi.login({ email, password }); onEnter(session); }
    catch { setError(t("login.unavailable")); }
  };
  return <main className="login-page">
    <section className="login-story">
      <img src="/logo-horizontal.svg" alt="IDAX" className="brand" />
      <p className="eyebrow">OPEN CORE / MODULAR BUSINESS PLATFORM</p>
      <h1>{t("login.headline")}</h1><p>{t("login.copy")}</p>
      <div className="signal"><span />{t("login.signal")}</div>
    </section>
    <section className="login-panel">
      <div className="language"><select aria-label={t("language")} value={locale} onChange={(e) => setLocale(e.target.value)}>{Object.entries(localeNames).map(([code, name]) => <option key={code} value={code}>{name}</option>)}</select></div>
      <form onSubmit={submit}><p className="kicker">{t("login.kicker")}</p><h2>{t("login.title")}</h2>
        <label>{t("login.email")}<input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoComplete="username" /></label>
        <label>{t("login.password")}<input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required autoComplete="current-password" /></label>
        {error && <p className="error">{error}</p>}<button type="submit">{t("login.submit")} <span>→</span></button>
        <button className="demo" type="button" onClick={() => onEnter({ user: { displayName: t("demo.user") }, tenants: [{ id: "demo", name: t("demo.tenant") }], demo: true })}>{t("login.demo")}</button>
      </form>
    </section>
  </main>;
}

function Workspace({ session, t, locale, setLocale, onLogout }) {
  const location = useLocation(); const navigate = useNavigate();
  const page = location.pathname.startsWith("/ledger") ? "ledger" : location.pathname.startsWith("/ostris") ? "ostris" : location.pathname.slice(1) || "home";
  const [sidebarWidth, setSidebarWidth] = useState(() => Number(localStorage.getItem("idax.sidebarWidth") || 248));
  const [resizing, setResizing] = useState(false);
  const [modules, setModules] = useState(demoModules);
  const [alertDraft, setAlertDraft] = useState(null);
  const tenants = session.tenants?.length ? session.tenants : [{ id: "default", name: t("demo.tenant") }];
  const [tenant, setTenant] = useState(tenants[0].id);
  useEffect(() => { if (!session.demo) shellApi.modules().then((items) => setModules(items.length ? items : demoModules)).catch(() => {}); }, [session.demo]);
  const title = page === "home" ? t("nav.home") : page === "modules" ? t("nav.modules") : t(`nav.${page}`);
  const beginResize = (event) => { event.preventDefault(); setResizing(true); const startX = event.clientX; const startWidth = sidebarWidth; const move = (e) => { const width = Math.max(188, Math.min(380, startWidth + e.clientX - startX)); setSidebarWidth(width); localStorage.setItem("idax.sidebarWidth", String(width)); }; const stop = () => { setResizing(false); window.removeEventListener("pointermove", move); window.removeEventListener("pointerup", stop); }; window.addEventListener("pointermove", move); window.addEventListener("pointerup", stop); };
  return <div className={`workspace ${resizing ? "is-resizing" : ""}`} style={{ "--sidebar-width": `${sidebarWidth}px` }}>
    <aside><div className="side-brand"><img src="/logo-mark.svg" alt="IDAX" /><span>IDAX</span></div><nav>{[["home","⌂"],["modules","◇"],["users","♙"],["roles","⌘"],["alerts","!"]].map(([id,glyph]) => <button key={id} className={page === id ? "active" : ""} onClick={() => navigate(id === "home" ? "/" : `/${id}`)} title={t(`nav.${id}`)}><i>{glyph}</i><span>{t(`nav.${id}`)}</span></button>)}</nav><button className="avatar" title={session.user?.displayName || "IDAX"}>{(session.user?.displayName || "I").slice(0,1)}</button><div className="sidebar-resize-handle" onPointerDown={beginResize} title={t("nav.resize")} /></aside>
    <div className="surface"><header><div><p>{t("workspace.label")}</p><select value={tenant} onChange={(e) => setTenant(e.target.value)}>{tenants.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</select></div><div className="header-actions"><select aria-label={t("language")} value={locale} onChange={(e) => setLocale(e.target.value)}>{Object.entries(localeNames).map(([code,name]) => <option key={code} value={code}>{name}</option>)}</select><button onClick={onLogout}>{t("logout")}</button></div></header>
      <section className="content"><p className="eyebrow">IDAX / {title.toUpperCase()}</p><h1>{title}</h1>
        {(page === "ledger" || page === "ostris") && <ModuleHost moduleKey={page} session={session} locale={locale} onBack={() => navigate("/")} />}
        {page === "home" && <><p className="lead">{t("home.lead")}</p><div className="metrics"><article><span>{t("home.modules")}</span><strong>{modules.length}</strong></article><article><span>{t("home.tenant")}</span><strong>{tenants.find((x) => x.id === tenant)?.name}</strong></article><article><span>{t("home.status")}</span><strong className="online">● {t("home.online")}</strong></article></div><ModuleGrid modules={modules} t={t} /></>}
        {page === "modules" && <><p className="lead">{t("modules.lead")}</p><ModuleGrid modules={modules} t={t} /></>}
        {(page === "users" || page === "roles" || page === "alerts") && <CrudWorkspace kind={page} tenantId={tenant} session={session} t={t} draft={page === "alerts" ? alertDraft : null} onNavigate={(target, draft) => { setAlertDraft(draft || null); navigate(`/${target}`); }} />}
      </section>
    </div>
  </div>;
}

function ModuleGrid({ modules, t }) { const navigate = useNavigate(); return <div className="module-grid">{modules.map((module) => <button onClick={() => navigate(module.route || `/${module.id}`)} key={module.id} className="module-card"><span className="module-glyph" style={{ color: module.color || "#65e6cc" }}>{module.glyph || module.id[0].toUpperCase()}</span><div><h2>{module.name || t(`module.${module.id}`)}</h2><p>{module.description || t(`module.${module.id}.copy`)}</p></div><b>↗</b></button>)}</div>; }

export default function App() {
  const [locale, setLocaleState] = useState(resolveLocale);
  const [session, setSession] = useState(null);
  const [checking, setChecking] = useState(true);
  const t = useMemo(() => (key) => translate(locale, key), [locale]);
  const setLocale = (value) => { localStorage.setItem("idax.locale", value); document.documentElement.lang = value; setLocaleState(value); };
  useEffect(() => { document.documentElement.lang = locale; shellApi.session().then(setSession).catch(() => {}).finally(() => setChecking(false)); }, []);
  if (checking) return <div className="loading"><img src="/logo-mark.svg" alt="IDAX" /></div>;
  if (!session) return <Login t={t} locale={locale} setLocale={setLocale} onEnter={setSession} />;
  return <Workspace session={session} t={t} locale={locale} setLocale={setLocale} onLogout={async () => { if (!session.demo) await shellApi.logout().catch(() => {}); setSession(null); }} />;
}
