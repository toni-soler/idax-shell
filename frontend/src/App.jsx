import React, { useEffect, useMemo, useState } from "react";
import { shellApi } from "./api.js";
import { localeNames, resolveLocale, translate } from "./i18n.js";
import { useLocation, useNavigate } from "react-router-dom";
import ModuleHost from "./ModuleHost.jsx";
import CrudWorkspace from "./CrudWorkspace.jsx";
import TenantAdmin from "./TenantAdmin.jsx";
import {matchExtension} from "./extensionRoutes.js";



// idax-core's LocalCredentialAuthenticator deliberately collapses every credential-related
// failure (unknown email, wrong password, disabled account) into the same 401
// INVALID_CREDENTIALS response, by design, so a login screen can never tell those apart without
// leaking account existence/status - this only distinguishes categories the backend genuinely
// exposes (see AuthenticationExceptionHandler in idax-shell's own backend).
function classifyLoginError(exception) {
  if (exception?.status === 401) return "login.invalidCredentials";
  if (exception?.status === 429) return "login.rateLimited";
  if (exception?.status >= 500) return "login.serviceUnavailable";
  if (exception?.status) return "login.unexpected";
  return "login.networkError";
}

// Second-step outcomes the backend reports with a stable code. An expired or unknown challenge or
// setup token cannot be retried: the flow starts again from the password.
const SECOND_STEP_EXPIRED = ["INVALID_MFA_CHALLENGE", "INVALID_MFA_SETUP_TOKEN", "MFA_SETUP_EXPIRED"];
function classifySecondStepError(exception) {
  if (SECOND_STEP_EXPIRED.includes(exception?.code)) return "mfa.expired";
  if (exception?.code === "INVALID_TOTP") return "mfa.invalidCode";
  if (exception?.code === "INVALID_RECOVERY_CODE") return "mfa.invalidRecovery";
  if (exception?.status === 429) return "login.rateLimited";
  if (exception?.status >= 500) return "login.serviceUnavailable";
  if (exception?.status) return "login.unexpected";
  return "login.networkError";
}
/** Groups the Base32 key in fours so it can be typed into an authenticator app. */
const groupSecret = (secret) => (secret ?? "").replace(/\s+/g, "").replace(/(.{4})/g, "$1 ").trim();

/**
 * Second sign-in step (MFA_REQUIRED / MFA_SETUP_REQUIRED). Codes, the setup key and the recovery
 * codes live only in this component's state: never stored, logged or sent anywhere but the shell
 * API. The session is stored and handed to the app only when the whole flow is complete - after a
 * forced setup, only once the user confirms the recovery codes are saved.
 */
function SecondStep({ t, step, onEnter, onCancel }) {
  const [code, setCode] = useState("");
  const [setup, setSetup] = useState(null);
  const [recovery, setRecovery] = useState(null);
  const [saved, setSaved] = useState(false);
  const [failure, setFailure] = useState(null);
  const [busy, setBusy] = useState(false);
  const forcedSetup = step.step === "MFA_SETUP";
  useEffect(() => {
    if (!forcedSetup) return undefined;
    let live = true;
    shellApi.startMfaSetup(step.setupToken).then((value) => live && setSetup(value), (exception) => live && setFailure(exception));
    return () => { live = false; };
  }, [step, forcedSetup]);
  const expired = SECOND_STEP_EXPIRED.includes(failure?.code);
  const submit = async (event) => {
    event.preventDefault(); setBusy(true); setFailure(null);
    try {
      if (!forcedSetup) { const session = await shellApi.verifyMfa(step.challengeToken, code.trim()); setCode(""); onEnter(session); return; }
      const result = await shellApi.activateMfaSetup(step.setupToken, code.trim());
      setCode(""); setSetup(null); setRecovery(result);
    } catch (exception) { setCode(""); setFailure(exception); } finally { setBusy(false); }
  };
  if (recovery) return <form data-testid="mfa-recovery" onSubmit={(event) => { event.preventDefault(); const session = recovery.session; setRecovery(null); onEnter(shellApi.enterSession(session)); }}>
    <p className="kicker">{t("login.kicker")}</p><h2>{t("mfa.recoveryTitle")}</h2>
    <p className="login-help">{t("mfa.recoveryHelp")}</p>
    <ol className="recovery-codes" data-testid="mfa-recovery-codes">{recovery.recoveryCodes.map((item) => <li key={item}><code>{item}</code></li>)}</ol>
    <label className="login-check"><input type="checkbox" checked={saved} onChange={(e) => setSaved(e.target.checked)} data-testid="mfa-recovery-saved" /><span>{t("mfa.recoverySaved")}</span></label>
    <button type="submit" disabled={!saved} data-testid="mfa-recovery-continue">{t("mfa.continue")} <span>→</span></button>
  </form>;
  return <form onSubmit={submit} data-testid={forcedSetup ? "mfa-setup" : "mfa-challenge"}>
    <p className="kicker">{t("login.kicker")}</p><h2>{t(forcedSetup ? "mfa.setupTitle" : "mfa.title")}</h2>
    <p className="login-help">{t(forcedSetup ? "mfa.setupHelp" : "mfa.help")}</p>
    {forcedSetup && setup && <div className="mfa-key"><span>{t("mfa.setupKey")}</span><code data-testid="mfa-setup-secret">{groupSecret(setup.secret)}</code>
      <a href={setup.otpauthUri} data-testid="mfa-setup-link">{t("mfa.setupOpenApp")}</a></div>}
    <label>{t(forcedSetup ? "mfa.setupCode" : "mfa.code")}<input value={code} onChange={(e) => setCode(e.target.value)} required disabled={expired}
      autoComplete="one-time-code" inputMode={forcedSetup ? "numeric" : "text"} maxLength={32} data-testid="mfa-code" /></label>
    {!forcedSetup && <p className="login-help">{t("mfa.recoveryHint")}</p>}
    {failure && <p className="error" role="alert" data-testid="mfa-error">{t(classifySecondStepError(failure))}</p>}
    {!expired && <button type="submit" disabled={busy || (forcedSetup && !setup)} data-testid="mfa-submit">{t("mfa.verify")} <span>→</span></button>}
    <button className="demo" type="button" onClick={onCancel} data-testid="mfa-cancel">{t(expired ? "mfa.restart" : "mfa.cancel")}</button>
  </form>;
}

function Login({ t, locale, setLocale, onEnter }) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [pending, setPending] = useState(null);
  const submit = async (event) => {
    event.preventDefault(); setError("");
    try {
      const outcome = await shellApi.login({ email, password });
      setPassword("");
      if (outcome.step === "SESSION") onEnter(outcome.session); else setPending(outcome);
    } catch (exception) { setError(t(classifyLoginError(exception))); }
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
      {pending ? <SecondStep t={t} step={pending} onEnter={onEnter} onCancel={() => setPending(null)} /> :
      <form onSubmit={submit}><p className="kicker">{t("login.kicker")}</p><h2>{t("login.title")}</h2>
        <label>{t("login.email")}<input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoComplete="username" /></label>
        <label>{t("login.password")}<input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required autoComplete="current-password" /></label>
        {error && <p className="error">{error}</p>}<button type="submit">{t("login.submit")} <span>→</span></button>
        <button className="demo" type="button" onClick={() => onEnter({ user: { displayName: t("demo.user") }, tenants: [{ id: "demo", name: t("demo.tenant") }], demo: true })}>{t("login.demo")}</button>
      </form>}
    </section>
  </main>;
}

function Workspace({ session, t, locale, setLocale, onLogout }) {
  const location = useLocation(); const navigate = useNavigate();
  const [modules, setModules] = useState([]);
  const extension = matchExtension(modules, location.pathname);
  const page = extension?.id || location.pathname.slice(1) || "home";
  const [sidebarWidth, setSidebarWidth] = useState(() => Number(localStorage.getItem("idax.sidebarWidth") || 248));
  const [sidebarCollapsed, setSidebarCollapsed] = useState(() => localStorage.getItem("idax.sidebarCollapsed") === "true");
  const [resizing, setResizing] = useState(false);
  const [alertDraft, setAlertDraft] = useState(null);
  const tenants = session.tenants?.length ? session.tenants : [{ id: "default", name: t("demo.tenant") }];
  const [tenant, setTenant] = useState(tenants[0].id);
  useEffect(() => { if (!session.demo) shellApi.modules().then((items) => setModules(items)).catch(() => {}); }, [session.demo]);
  const title = page === "home" ? t("nav.home") : page === "modules" ? t("nav.modules") : (extension?.name || t(`nav.${page}`));
  const beginResize = (event) => { event.preventDefault(); setResizing(true); const startX = event.clientX; const startWidth = sidebarWidth; const move = (e) => { const width = Math.max(188, Math.min(380, startWidth + e.clientX - startX)); setSidebarWidth(width); localStorage.setItem("idax.sidebarWidth", String(width)); }; const stop = () => { setResizing(false); window.removeEventListener("pointermove", move); window.removeEventListener("pointerup", stop); }; window.addEventListener("pointermove", move); window.addEventListener("pointerup", stop); };
  const toggleSidebar = () => { const next = !sidebarCollapsed; setSidebarCollapsed(next); localStorage.setItem("idax.sidebarCollapsed", String(next)); };
  return <div className={`workspace ${resizing ? "is-resizing" : ""} ${sidebarCollapsed ? "is-collapsed" : ""}`} style={{ "--sidebar-width": `${sidebarCollapsed ? 72 : sidebarWidth}px` }}>
    <aside><div className="side-brand"><img className="brand-expanded" src="/logo-horizontal-sidebar.svg" alt="IDAX" /><img className="brand-collapsed" src="/logo-monochrome.svg" alt="IDAX" /><button type="button" className="sidebar-collapse" onClick={toggleSidebar} title={t(sidebarCollapsed ? "nav.expand" : "nav.collapse")} aria-label={t(sidebarCollapsed ? "nav.expand" : "nav.collapse")}>{sidebarCollapsed ? "›" : "‹"}</button></div><nav>{[["home","⌂"],["modules","◇"],["users","♙"],["roles","⌘"],["alerts","!"], ...(session.user?.superuser ? [["tenants","▦"]] : [])].map(([id,glyph]) => <button key={id} className={page === id ? "active" : ""} onClick={() => navigate(id === "home" ? "/" : `/${id}`)} title={t(`nav.${id}`)}><i>{glyph}</i><span>{t(`nav.${id}`)}</span></button>)}</nav><button className="avatar" title={session.user?.displayName || "IDAX"}>{(session.user?.displayName || "I").slice(0,1)}</button>{!sidebarCollapsed && <div className="sidebar-resize-handle" onPointerDown={beginResize} title={t("nav.resize")} />}</aside>
    <div className="surface"><header><div><p>{t("workspace.label")}</p><select value={tenant} onChange={(e) => setTenant(e.target.value)}>{tenants.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</select></div><div className="header-actions"><select aria-label={t("language")} value={locale} onChange={(e) => setLocale(e.target.value)}>{Object.entries(localeNames).map(([code,name]) => <option key={code} value={code}>{name}</option>)}</select><button onClick={onLogout}>{t("logout")}</button></div></header>
      <section className="content"><p className="eyebrow">IDAX / {title.toUpperCase()}</p><h1>{title}</h1>
        {extension && <ModuleHost key={`${extension.id}:${tenant}`} moduleKey={extension.id} session={{...session,activeTenantId:tenant}} locale={locale} onBack={() => navigate("/")} />}
        {page === "home" && <><p className="lead">{t("home.lead")}</p><div className="metrics"><article><span>{t("home.modules")}</span><strong>{modules.length}</strong></article><article><span>{t("home.tenant")}</span><strong>{tenants.find((x) => x.id === tenant)?.name}</strong></article><article><span>{t("home.status")}</span><strong className="online">● {t("home.online")}</strong></article></div><ModuleGrid modules={modules} t={t} /></>}
        {page === "modules" && <><p className="lead">{t("modules.lead")}</p><ModuleGrid modules={modules} t={t} /></>}
        {(page === "users" || page === "roles" || page === "alerts") && <CrudWorkspace kind={page} tenantId={tenant} session={session} t={t} draft={page === "alerts" ? alertDraft : null} onNavigate={(target, draft) => { setAlertDraft(draft || null); navigate(`/${target}`); }} />}
        {page === "tenants" && session.user?.superuser && <TenantAdmin t={t} />}
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
