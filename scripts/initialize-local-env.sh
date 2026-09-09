#!/usr/bin/env sh
set -eu

repo_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
secret_root="$repo_root/.local/secrets"
maven_root="$repo_root/.local/maven-repository"
core_runtime_root=$(CDPATH= cd -- "$repo_root/../idax-core-runtime" && pwd)
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

core_pom="$maven_root/maven2/es/idynamicsax/idax/idax-core/0.3.0/idax-core-0.3.0.pom"
rm -rf "$maven_root"
mkdir -p "$maven_root"
git -C "$core_runtime_root" archive gh-pages maven2 | tar -xf - -C "$maven_root"
[ -f "$core_pom" ] || {
  echo "IDAX Core 0.3.0 is not available in the local gh-pages branch." >&2
  exit 1
}
sha1sum "$core_pom" | awk '{print $1}' > "$core_pom.sha1"
sha256sum "$core_pom" | awk '{print $1}' > "$core_pom.sha256"

maven_container=idax-open-core-local-maven
if docker ps -a --filter "name=^/$maven_container$" --format '{{.Names}}' | grep -q .; then
  docker rm -f "$maven_container" >/dev/null
fi
docker run -d --name "$maven_container" -p 127.0.0.1:8766:80 \
  --mount "type=bind,source=$maven_root,target=/usr/share/nginx/html,readonly" nginx:1.27-alpine >/dev/null

echo "Local secrets and the Core 0.3 Maven repository are ready (ignored by Git)."
