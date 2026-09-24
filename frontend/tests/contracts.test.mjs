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
  assert.match(crud, /filter\.criteria\.every/);
  assert.match(crud, /filters: values/);
  assert.match(crud, /Object\.entries\(filters\)/);
  assert.match(crud, /kind === "users" && field\.key === "role"/);
  assert.match(crud, /next\.subject = next\.subject \|\| next\.email/);
  assert.match(crud, /setError\(""\); setEditing\(null\); setCreating\(null\)/);
  assert.match(crud, /aria-label=\{t\("crud\.cancel"\)\}/);
  assert.match(api, /\/api\/shell\/v1\/tenants\/\$\{encodeURIComponent\(tenantId\)\}\/alerts/);
  assert.match(api, /details\?\.message/);
  assert.doesNotMatch(crud, /idax_core|JdbcTemplate|\/api\/legacy/i);
});

import {matchExtension} from "../src/extensionRoutes.js";
test("module matching uses manifest route, not id or substring",()=>{const modules=[{id:"orchard",route:"/community/garden"}];assert.equal(matchExtension(modules,"/community/garden/new").id,"orchard");assert.equal(matchExtension(modules,"/community/gardening"),undefined);assert.equal(matchExtension(modules,"/orchard"),undefined);});

test("module extensions receive the active tenant as a header, not just on the SDK object",()=>{
  const sdk = read("../src/moduleSdk.js");
  assert.match(sdk, /activeTenantId: session\.activeTenantId/);
  assert.match(sdk, /"X-Tenant":\s*session\.activeTenantId/);
});

test("the Users editor actually assigns roleIds, not just the legacy role label",()=>{
  const crud = read("../src/CrudWorkspace.jsx");
  assert.match(crud, /shellApi\.saveUserRoles/);
  assert.match(crud, /shellApi\.roleUsers/);
  assert.match(crud, /type: "userRoles"/);
});

test("the Roles editor keeps permissions as an array and never round-trips through a joined string",()=>{
  const crud = read("../src/CrudWorkspace.jsx");
  assert.match(crud, /permissions: Array\.isArray\(initial\?\.permissions\) \? initial\.permissions : \[\]/);
  assert.doesNotMatch(crud, /\.join\(", "\)/);
  assert.doesNotMatch(crud, /permissions\.split\(","\)/);
  // toggling one permission must only add/remove that code, never replace the whole set
  assert.match(crud, /permissions: checked \? \[\.\.\.permissions, code\] : permissions\.filter\(\(existing\) => existing !== code\)/);
});

test("the Roles editor blocks saving and surfaces an error when the permission catalog fails to load",()=>{
  const crud = read("../src/CrudWorkspace.jsx");
  assert.match(crud, /setPermissionCatalogError\(true\)/);
  assert.match(crud, /const blockSave = kind === "roles" && Boolean\(permissionCatalogError\)/);
  assert.match(crud, /<button disabled=\{blockSave\}>\{t\("crud\.save"\)\}<\/button>/);
  // a failed per-role permissions fetch must not silently open the editor with an empty list
  assert.match(crud, /catch \{ setError\(t\("field\.permissionsLoadError"\)\); \}/);
});

test("login failures are classified instead of always blamed on a disconnected service",()=>{
  const app = read("../src/App.jsx");
  const en = JSON.parse(read("../src/locales/en.json"));
  assert.doesNotMatch(app, /login\.unavailable/);
  assert.doesNotMatch(app, /catch \{ setError\(t\("login\.unavailable"\)\); \}/);
  assert.match(app, /exception\?\.status === 401.*login\.invalidCredentials/);
  assert.match(app, /exception\?\.status === 429.*login\.rateLimited/);
  assert.match(app, /exception\?\.status >= 500.*login\.serviceUnavailable/);
  assert.match(app, /MFA_REQUIRED.*login\.mfaUnsupported/);
  for (const key of ["login.invalidCredentials", "login.rateLimited", "login.serviceUnavailable", "login.unexpected", "login.networkError", "login.mfaUnsupported"]) {
    assert.ok(en[key], `en.json is missing ${key}`);
  }
  assert.equal(en["login.unavailable"], undefined, "dead key should have been removed, not left orphaned");
});

test("api.js attaches the real HTTP status to a failed request instead of just a message string",()=>{
  const api = read("../src/api.js");
  assert.match(api, /error\.status = response\.status/);
});
