import fs from "node:fs";
import path from "node:path";
import process from "node:process";

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
console.log(`Validated ${reference.length} keys in ${files.length} locales.`);
