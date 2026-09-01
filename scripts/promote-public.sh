#!/usr/bin/env sh
set -eu
root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
source_dir="$root/vendor/idax-core-runtime"
test -d "$source_dir/.git" || { echo 'Clone the declared public source into vendor/idax-core-runtime first.' >&2; exit 1; }
test "$(git -C "$source_dir" remote get-url origin)" = 'https://github.com/toni-soler/idax-core-runtime.git' || { echo 'Source remote is not allow-listed.' >&2; exit 1; }
git -C "$source_dir" fetch origin main
if rg -n -i '(/workspace/|BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY|password[[:space:]]*[:=][[:space:]]*[^$<{])' "$source_dir/database/migration-idax-core"; then echo 'Promotion blocked by exposure scan.' >&2; exit 1; fi
test "${1:-}" = '--apply' || { echo 'Dry run passed. Re-run with --apply.'; exit 0; }
mkdir -p "$root/vendor/idax-core-runtime"
cp -R "$source_dir/database" "$root/vendor/idax-core-runtime/"
echo 'Promoted allow-listed public migrations.'

