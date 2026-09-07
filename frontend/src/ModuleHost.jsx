import React, { useEffect, useState } from "react";
import { installModuleSdk, loadModuleExtension } from "./moduleSdk.js";

export default function ModuleHost({ moduleKey, session, locale, onBack }) {
  const [Component, setComponent] = useState(() => window.__IDAX_MODULE_EXTENSIONS__?.[moduleKey]?.component || null);
  const [error, setError] = useState(null);
  useEffect(() => { installModuleSdk(session, locale); loadModuleExtension(moduleKey).then((loaded) => setComponent(() => loaded)).catch(setError); }, [moduleKey, session, locale]);
  if (error) return <section className="module-load-error"><button onClick={onBack}>← IDAX</button><h2>No se pudo cargar {moduleKey}</h2><p>{error.message}</p></section>;
  if (!Component) return <div className="module-loading">Cargando {moduleKey}…</div>;
  return <Component />;
}
