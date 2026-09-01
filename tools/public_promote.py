#!/usr/bin/env python3
"""Deterministic, review-only exporter for explicitly allow-listed components."""
from __future__ import annotations

import argparse, fnmatch, hashlib, json, os, re, shutil, subprocess, sys, tempfile
from pathlib import Path


def load(path: Path) -> dict:
    data=json.loads(path.read_text(encoding="utf-8"))
    required={"schemaVersion","component","source","destination","include","exclude","transformations","license","forbidden","generatedFiles","checks"}
    if data.get("schemaVersion") != 1 or set(data) != required:
        raise ValueError("manifest must be a closed v1 object")
    if not re.fullmatch(r"[a-z0-9][a-z0-9-]+", data["component"]): raise ValueError("invalid component id")
    return data


def git(root: Path, *args: str) -> str:
    return subprocess.check_output(["git","-C",str(root),*args], text=True, stderr=subprocess.STDOUT).strip()


def matches(path: str, patterns: list[str]) -> bool:
    return any(fnmatch.fnmatch(path,p) or fnmatch.fnmatch(path.removeprefix("./"),p) for p in patterns)


def selected(source: Path, manifest: dict) -> list[tuple[Path,str]]:
    roots=[(source / p).resolve() for p in manifest["source"]["paths"]]
    result=[]
    for root in roots:
        if source.resolve() not in (root,*root.parents): raise ValueError("source path escapes repository")
        for file in sorted(p for p in root.rglob("*") if p.is_file() and ".git" not in p.parts):
            rel=file.relative_to(source).as_posix()
            if matches(rel,manifest["include"]) and not matches(rel,manifest["exclude"]): result.append((file,rel))
    if not result: raise ValueError("allowlist selected no files")
    return result


def transform(data: bytes, rules: list[dict]) -> bytes:
    for rule in rules:
        if rule["id"] == "normalize-text" and rule["version"] == 1:
            text=data.decode("utf-8").replace("\r\n","\n").replace("\r","\n")
            data=(text.rstrip()+"\n").encode()
        else: raise ValueError(f"unapproved transformation: {rule}")
    return data


def scan(tree: Path, manifest: dict) -> None:
    forbidden=manifest["forbidden"]
    patterns=[*forbidden["patterns"],*forbidden["imports"],*forbidden["dependencies"]]
    builtin=[r"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----",r"(?:C:\\Users\\|/workspace/)"]
    compiled=[re.compile(p,re.I) for p in [*patterns,*builtin]]
    findings=[]
    for file in sorted(p for p in tree.rglob("*") if p.is_file()):
        try: text=file.read_text(encoding="utf-8")
        except UnicodeDecodeError: continue
        for regex in compiled:
            if regex.search(text): findings.append(f"{file.relative_to(tree)}: {regex.pattern}")
    if findings: raise ValueError("forbidden content:\n"+"\n".join(findings))

    generated=manifest["generatedFiles"]
    generated_hits=[p.relative_to(tree).as_posix() for p in tree.rglob("*") if p.is_file() and matches(p.relative_to(tree).as_posix(),generated.get("patterns",[]))]
    if generated["policy"] == "reject" and generated_hits:
        raise ValueError("generated files rejected:\n"+"\n".join(generated_hits))


def digest(tree: Path) -> str:
    h=hashlib.sha256()
    for file in sorted(p for p in tree.rglob("*") if p.is_file()):
        h.update(file.relative_to(tree).as_posix().encode()+b"\0"+file.read_bytes()+b"\0")
    return h.hexdigest()


def run_checks(tree: Path, checks: dict) -> None:
    for name in ("format","build","test","secretScan","licenseScan"):
        command=checks[name]
        subprocess.run(command,cwd=tree,check=True,shell=False)


def main() -> int:
    parser=argparse.ArgumentParser()
    parser.add_argument("manifest",type=Path); parser.add_argument("--source-root",type=Path,required=True)
    parser.add_argument("--destination-root",type=Path,required=True); parser.add_argument("--apply",action="store_true")
    parser.add_argument("--repository-registry",type=Path,required=True,help="trusted JSON alias-to-remote-regex registry")
    parser.add_argument("--report",type=Path)
    args=parser.parse_args(); manifest=load(args.manifest)
    source=args.source_root.resolve(); destination=args.destination_root.resolve()
    registry=json.loads(args.repository_registry.read_text(encoding="utf-8"))
    repository_id=manifest["source"]["repositoryId"]
    if repository_id not in registry: raise ValueError("source repository alias is not in the trusted registry")
    remote=git(source,"remote","get-url","origin")
    if not re.fullmatch(registry[repository_id],remote): raise ValueError("source remote does not match trusted registry")
    destination_path=manifest["destination"]["path"]
    if destination_path in {"", ".", "/"} or Path(destination_path).is_absolute() or ".." in Path(destination_path).parts:
        raise ValueError("destination.path must be a non-root relative directory")
    actual=git(source,"rev-parse","HEAD"); expected=manifest["source"]["ref"]
    if actual != expected: raise ValueError(f"source commit mismatch: expected {expected}, got {actual}")
    if git(destination,"branch","--show-current") in {"main","master"} and args.apply:
        raise ValueError("refusing to apply on the default branch; create a review branch")
    with tempfile.TemporaryDirectory(prefix="idax-public-promote-") as tmp:
        stage=Path(tmp)/"tree"; stage.mkdir()
        for file,rel in selected(source,manifest):
            output=stage/rel; output.parent.mkdir(parents=True,exist_ok=True); output.write_bytes(transform(file.read_bytes(),manifest["transformations"]))
        scan(stage,manifest); run_checks(stage,manifest["checks"])
        tree_digest=digest(stage)
        current=Path(tmp)/"current"; current.mkdir()
        dest_path=destination/destination_path
        if dest_path.exists(): shutil.copytree(dest_path,current,dirs_exist_ok=True)
        drift=digest(current)!=tree_digest
        report={"schemaVersion":1,"component":manifest["component"],"sourceCommit":actual,"treeSha256":tree_digest,"drift":drift,"applied":bool(args.apply and drift)}
        if args.apply and drift:
            if dest_path.exists(): shutil.rmtree(dest_path)
            shutil.copytree(stage,dest_path)
        rendered=json.dumps(report,indent=2,sort_keys=True)+"\n"
        if args.report: args.report.write_text(rendered,encoding="utf-8",newline="\n")
        print(rendered,end="")
        return 2 if drift and not args.apply else 0


if __name__ == "__main__":
    try: raise SystemExit(main())
    except (ValueError,subprocess.CalledProcessError) as exc: print(f"promotion failed: {exc}",file=sys.stderr); raise SystemExit(1)
