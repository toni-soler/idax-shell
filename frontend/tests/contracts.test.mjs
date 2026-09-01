import test from"node:test";import assert from"node:assert/strict";import fs from"node:fs";
test("checkpoint contains no environment-specific references",()=>{const source=fs.readFileSync(new URL("../index.html",import.meta.url),"utf8");assert.doesNotMatch(source,/idax-legacy|localhost|C:\\\\/i)});
