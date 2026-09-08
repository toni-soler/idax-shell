import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";

const read = (path) => fs.readFileSync(new URL(path, import.meta.url), "utf8");

test("public shell contains no private or machine-specific references", () => {
  const source = [read("../index.html"), read("../src/App.jsx"), read("../src/api.js")].join("\n");
  assert.doesNotMatch(source, /idax-legacy|santahelenasolutions|C:\\\\/i);
});

test("API calls stay behind the versioned shell boundary", () => {
  const source = read("../src/api.js");
  assert.match(source, /\/api\/shell\/v1\/auth\/login/);
  assert.match(source, /\/api\/shell\/v1\/extensions/);
  assert.match(source, /sessionStorage\.setItem\("idax\.accessToken"/);
  assert.match(source, /const isLogin = path === "\/api\/shell\/v1\/auth\/login"/);
  assert.match(source, /sessionStorage\.removeItem\("idax\.accessToken"\)/);
  assert.doesNotMatch(source, /\/api\/legacy/i);
});

test("administration reuses one generic CRUD workspace and versioned APIs", () => {
  const app = read("../src/App.jsx");
  const crud = read("../src/CrudWorkspace.jsx");
  const api = read("../src/api.js");
  assert.match(app, /<CrudWorkspace kind=\{page\}/);
  assert.match(app, /\["alerts","!"\]/);
  assert.match(crud, /const definitions = \{[\s\S]*users:[\s\S]*roles:[\s\S]*alerts:/);
  assert.match(crud, /savedFilters/);
  assert.match(api, /\/api\/shell\/v1\/tenants\/\$\{encodeURIComponent\(tenantId\)\}\/alerts/);
  assert.doesNotMatch(crud, /idax_core|JdbcTemplate|\/api\/legacy/i);
});
