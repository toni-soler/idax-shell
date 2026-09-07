import fs from "node:fs";
import path from "node:path";
import process from "node:process";
import { adminLocales } from "../src/adminLocales.js";

const directory = new URL("../src/locales/", import.meta.url);
const files = fs.readdirSync(directory).filter((name) => name.endsWith(".json"));
const reference = Object.keys(JSON.parse(fs.readFileSync(new URL("en.json", directory), "utf8"))).sort();
const failures = [];
for (const file of files) {
  const keys = Object.keys(JSON.parse(fs.readFileSync(new URL(file, directory), "utf8"))).sort();
  if (keys.join("\n") !== reference.join("\n")) failures.push(path.basename(file));
}
if (files.length !== 12 || failures.length) {
  console.error(`Expected 12 complete locales; incomplete: ${failures.join(", ") || "none"}`);
  process.exit(1);
}
const adminReference = Object.keys(adminLocales.en).sort();
const adminFailures = Object.entries(adminLocales).filter(([, messages]) => {
  const keys = Object.keys(messages).sort();
  return keys.join("\n") !== adminReference.join("\n") || keys.some((key) => typeof messages[key] !== "string" || !messages[key].trim());
}).map(([locale]) => locale);
if (Object.keys(adminLocales).length !== 12 || adminFailures.length) {
  console.error(`Expected 12 complete administration locales; incomplete: ${adminFailures.join(", ") || "none"}`);
  process.exit(1);
}
console.log(`Validated ${reference.length + adminReference.length} keys in ${files.length} locales.`);
