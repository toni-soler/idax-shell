import React from "react";
import * as router from "react-router-dom";
import { locales } from "./i18n.js";

const resources = Object.fromEntries(Object.keys(locales).map((locale) => [locale, structuredClone(locales[locale])]));
const merge = (target, source) => Object.entries(source || {}).forEach(([key, value]) => {
  if (value && typeof value === "object" && !Array.isArray(value)) { target[key] ||= {}; merge(target[key], value); } else target[key] = value;
});
const read = (object, key) => key.split(".").reduce((value, part) => value?.[part], object);

export function installModuleSdk(session, locale) {
  window.__IDAX_MODULE_SDK__ = {
    React, router, activeTenantId: session.activeTenantId, user: session.user, demo: Boolean(session.demo),
    i18n: { addResourceBundle(language, _namespace, bundle) { resources[language] ||= {}; merge(resources[language], bundle); }, t(key, fallback) { return read(resources[locale], key) ?? read(resources.en, key) ?? fallback ?? key; } },
    // Shell's own CrudWorkspace embeds the active tenant in the URL path; module extensions
    // (Ledger, osTRIS) instead read it from this X-Tenant header - without it, a module backend
    // that requires an explicit tenant (e.g. Ledger's Proofs) falls back to the JWT's own tenant
    // claim, which is absent for this session's admin identity, and rejects the request.
    fetchWithAuth(path, options = {}) { const token = sessionStorage.getItem("idax.accessToken"); return fetch(path, { ...options, credentials: "include", headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}), ...(session.activeTenantId ? { "X-Tenant": session.activeTenantId } : {}), ...options.headers } }); },
    useAuth() { const permissions = new Set(session.user?.permissions || []); return { isSuperuser: Boolean(session.user?.superuser), hasPermission: (permission) => permissions.has(permission) }; },
  };
}

export function loadModuleExtension(moduleKey) {
  const existing = window.__IDAX_MODULE_EXTENSIONS__?.[moduleKey]?.component;
  if (existing) return Promise.resolve(existing);
  return new Promise((resolve, reject) => { const style = document.createElement("link"); style.rel = "stylesheet"; style.href = `/extensions/${moduleKey}/index.css`; document.head.appendChild(style); const script = document.createElement("script"); script.src = `/extensions/${moduleKey}/index.js`; script.onload = () => { const component = window.__IDAX_MODULE_EXTENSIONS__?.[moduleKey]?.component; component ? resolve(component) : reject(new Error(`Extension ${moduleKey} did not register`)); }; script.onerror = () => reject(new Error(`Extension ${moduleKey} could not be loaded`)); document.head.appendChild(script); });
}
