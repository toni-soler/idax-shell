[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$secretRoot = Join-Path $repoRoot ".local\secrets"
New-Item -ItemType Directory -Force -Path $secretRoot | Out-Null

function Initialize-SecretFile {
    param(
        [Parameter(Mandatory)] [string] $Name,
        [Parameter(Mandatory)] [scriptblock] $Create
    )

    $path = Join-Path $secretRoot $Name
    if (Test-Path -LiteralPath $path -PathType Container) {
        $children = @(Get-ChildItem -LiteralPath $path -Force)
        if ($children.Count -ne 0) {
            throw "The expected secret file is a non-empty directory: $path"
        }
        Remove-Item -LiteralPath $path
    }
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        & $Create $path
    }
}

if (-not (Get-Command openssl -ErrorAction SilentlyContinue)) {
    throw "OpenSSL is required and was not found on PATH."
}

Initialize-SecretFile "postgres_password" {
    param($path)
    Set-Content -LiteralPath $path -NoNewline -Value "idax-local-only"
}
Initialize-SecretFile "jwt_private_key" {
    param($path)
    & openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out $path
    if ($LASTEXITCODE -ne 0) { throw "OpenSSL could not generate the private key." }
}
Initialize-SecretFile "jwt_public_key" {
    param($path)
    $privateKey = Join-Path $secretRoot "jwt_private_key"
    & openssl pkey -in $privateKey -pubout -out $path
    if ($LASTEXITCODE -ne 0) { throw "OpenSSL could not generate the public key." }
}
Initialize-SecretFile "bootstrap_token" {
    param($path)
    $bytes = New-Object byte[] 32
    [Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
    Set-Content -LiteralPath $path -NoNewline -Value ([Convert]::ToHexString($bytes).ToLowerInvariant())
}

Write-Output "Local secrets are ready under .local/secrets (ignored by Git)."
