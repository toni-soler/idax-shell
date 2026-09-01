param([switch]$Apply)
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$source = Join-Path $root 'vendor/idax-core-runtime'
if (-not (Test-Path (Join-Path $source '.git'))) { throw 'Clone the declared public source into vendor/idax-core-runtime first.' }
$remote = git -C $source remote get-url origin
if ($remote -ne 'https://github.com/toni-soler/idax-core-runtime.git') { throw 'Source remote is not the allow-listed public repository.' }
git -C $source fetch --ff-only origin main
if ($LASTEXITCODE -ne 0) { throw 'Public source fetch failed.' }
$forbidden = rg -n -i 'C:\\Users\\|/workspace/|BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY|password\s*[:=]\s*[^$<{]' (Join-Path $source 'database/migration-idax-core')
if ($LASTEXITCODE -eq 0) { throw "Promotion blocked by exposure scan:`n$forbidden" }
if (-not $Apply) { Write-Output 'Dry run passed. Re-run with -Apply to update the vendored public migrations.'; exit 0 }
Copy-Item -Recurse -Force (Join-Path $source 'database') (Join-Path $root 'vendor/idax-core-runtime')
Write-Output 'Promoted allow-listed public migrations.'

