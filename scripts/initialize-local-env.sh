#!/usr/bin/env sh
set -eu

repo_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
secret_root="$repo_root/.local/secrets"
mkdir -p "$secret_root"

require_file_path() {
  path=$1
  if [ -d "$path" ]; then
    if [ -n "$(ls -A "$path")" ]; then
      echo "The expected secret file is a non-empty directory: $path" >&2
      exit 1
    fi
    rmdir "$path"
  fi
}

command -v openssl >/dev/null 2>&1 || {
  echo "OpenSSL is required and was not found on PATH." >&2
  exit 1
}

for name in postgres_password jwt_private_key jwt_public_key bootstrap_token; do
  require_file_path "$secret_root/$name"
done

[ -f "$secret_root/postgres_password" ] || printf %s 'idax-local-only' > "$secret_root/postgres_password"
[ -f "$secret_root/jwt_private_key" ] || openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out "$secret_root/jwt_private_key"
[ -f "$secret_root/jwt_public_key" ] || openssl pkey -in "$secret_root/jwt_private_key" -pubout -out "$secret_root/jwt_public_key"
[ -f "$secret_root/bootstrap_token" ] || openssl rand -hex 32 > "$secret_root/bootstrap_token"
chmod 600 "$secret_root"/*

echo "Local secrets are ready under .local/secrets (ignored by Git)."
