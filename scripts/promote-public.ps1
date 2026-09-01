param([Parameter(ValueFromRemainingArguments=$true)][string[]]$Arguments)
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
& python (Join-Path $root 'tools/public_promote.py') @Arguments
exit $LASTEXITCODE
