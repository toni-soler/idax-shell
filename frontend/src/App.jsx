import React, { useEffect, useMemo, useState } from "react";
import { shellApi } from "./api.js";
import { localeNames, resolveLocale, translate } from "./i18n.js";

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
  const [page, setPage] = useState("home");
  const [modules, setModules] = useState(demoModules);
  const tenants = session.tenants?.length ? session.tenants : [{ id: "default", name: t("demo.tenant") }];
  const [tenant, setTenant] = useState(tenants[0].id);
  useEffect(() => { if (!session.demo) shellApi.modules().then((items) => setModules(items.length ? items : demoModules)).catch(() => {}); }, [session.demo]);
  const title = page === "home" ? t("nav.home") : page === "modules" ? t("nav.modules") : t(`nav.${page}`);
  return <div className="workspace">
    <aside><img src="/logo-mark.svg" alt="IDAX" /><nav>{[["home","⌂"],["modules","◇"],["users","♙"],["roles","⌘"]].map(([id,glyph]) => <button key={id} className={page === id ? "active" : ""} onClick={() => setPage(id)} title={t(`nav.${id}`)}>{glyph}</button>)}</nav><button className="avatar" title={session.user?.displayName || "IDAX"}>{(session.user?.displayName || "I").slice(0,1)}</button></aside>
    <div className="surface"><header><div><p>{t("workspace.label")}</p><select value={tenant} onChange={(e) => setTenant(e.target.value)}>{tenants.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</select></div><div className="header-actions"><select aria-label={t("language")} value={locale} onChange={(e) => setLocale(e.target.value)}>{Object.entries(localeNames).map(([code,name]) => <option key={code} value={code}>{name}</option>)}</select><button onClick={onLogout}>{t("logout")}</button></div></header>
      <section className="content"><p className="eyebrow">IDAX / {title.toUpperCase()}</p><h1>{title}</h1>
        {page === "home" && <><p className="lead">{t("home.lead")}</p><div className="metrics"><article><span>{t("home.modules")}</span><strong>{modules.length}</strong></article><article><span>{t("home.tenant")}</span><strong>{tenants.find((x) => x.id === tenant)?.name}</strong></article><article><span>{t("home.status")}</span><strong className="online">● {t("home.online")}</strong></article></div><ModuleGrid modules={modules} t={t} /></>}
        {page === "modules" && <><p className="lead">{t("modules.lead")}</p><ModuleGrid modules={modules} t={t} /></>}
        {(page === "users" || page === "roles") && <section className="empty"><span>{page === "users" ? "♙" : "⌘"}</span><h2>{t(`${page}.title`)}</h2><p>{session.demo ? t("admin.demo") : t("admin.ready")}</p></section>}
      </section>
    </div>
  </div>;
}

function ModuleGrid({ modules, t }) { return <div className="module-grid">{modules.map((module) => <a href={module.route || "#"} key={module.id} className="module-card"><span className="module-glyph" style={{ color: module.color || "#65e6cc" }}>{module.glyph || module.id[0].toUpperCase()}</span><div><h2>{module.name || t(`module.${module.id}`)}</h2><p>{module.description || t(`module.${module.id}.copy`)}</p></div><b>↗</b></a>)}</div>; }

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
