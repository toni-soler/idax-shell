import es from "./locales/es.json";
import en from "./locales/en.json";
import ca from "./locales/ca.json";
import de from "./locales/de.json";
import fr from "./locales/fr.json";
import it from "./locales/it.json";
import pt from "./locales/pt.json";
import jp from "./locales/jp.json";
import eu from "./locales/eu.json";
import gl from "./locales/gl.json";
import ptBR from "./locales/pt-BR.json";
import zh from "./locales/zh.json";
import { adminLocales } from "./adminLocales.js";

const baseLocales = { es, en, ca, de, fr, it, pt, jp, eu, gl, "pt-BR": ptBR, zh };
export const locales = Object.fromEntries(Object.entries(baseLocales).map(([code, messages]) => [code, { ...messages, ...adminLocales[code] }]));
export const localeNames = { es: "Español", en: "English", ca: "Català", de: "Deutsch", fr: "Français", it: "Italiano", pt: "Português", jp: "日本語", eu: "Euskara", gl: "Galego", "pt-BR": "Português (Brasil)", zh: "中文" };

export function resolveLocale() {
  const saved = localStorage.getItem("idax.locale");
  if (locales[saved]) return saved;
  const browser = navigator.language;
  if (locales[browser]) return browser;
  return locales[browser?.split("-")[0]] ? browser.split("-")[0] : "en";
}

export function translate(locale, key) {
  return locales[locale]?.[key] ?? locales.en[key] ?? key;
}
