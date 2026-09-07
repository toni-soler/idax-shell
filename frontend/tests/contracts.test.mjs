import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";

const read = (path) => fs.readFileSync(new URL(path, import.meta.url), "utf8");

test("public shell contains no private or machine-specific references", () => {
  const source = [read("../index.html"), read("../src/App.jsx"), read("../src/api.js")].join("\n");
  assert.doesNotMatch(source, /idax-legacy|santahelenasolutions|C:\\\\|antoni\.soler@ingubu\.io/i);
});

test("API calls stay behind the versioned shell boundary", () => {
  const source = read("../src/api.js");
  assert.match(source, /\/api\/shell\/v1\/auth\/login/);
  assert.match(source, /\/api\/shell\/v1\/extensions/);
  assert.doesNotMatch(source, /\/api\/legacy/i);
});
