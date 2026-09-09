[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$secretRoot = Join-Path $repoRoot ".local\secrets"
$mavenRoot = Join-Path $repoRoot ".local\maven-repository"
$coreRuntimeRoot = Join-Path (Split-Path -Parent $repoRoot) "idax-core-runtime"
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

$corePom = Join-Path $mavenRoot "maven2\es\idynamicsax\idax\idax-core\0.3.0\idax-core-0.3.0.pom"
if (-not (Test-Path -LiteralPath (Join-Path $coreRuntimeRoot ".git"))) {
    throw "The sibling idax-core-runtime repository was not found: $coreRuntimeRoot"
}
if (Test-Path -LiteralPath $mavenRoot) { Remove-Item -LiteralPath $mavenRoot -Recurse -Force }
New-Item -ItemType Directory -Force -Path $mavenRoot | Out-Null
$archive = Join-Path $repoRoot ".local\idax-core-maven.tar"
& git -C $coreRuntimeRoot archive --format=tar --output=$archive gh-pages maven2
if ($LASTEXITCODE -ne 0) { throw "Could not export the local Core Maven repository from gh-pages." }
& tar -xf $archive -C $mavenRoot
if ($LASTEXITCODE -ne 0) { throw "Could not extract the local Core Maven repository." }
Remove-Item -LiteralPath $archive
if (-not (Test-Path -LiteralPath $corePom -PathType Leaf)) {
    throw "IDAX Core 0.3.0 is not available in the local gh-pages branch."
}
$localPomSha1 = (Get-FileHash -LiteralPath $corePom -Algorithm SHA1).Hash.ToLowerInvariant()
$localPomSha256 = (Get-FileHash -LiteralPath $corePom -Algorithm SHA256).Hash.ToLowerInvariant()
Set-Content -LiteralPath "$corePom.sha1" -Value $localPomSha1 -NoNewline
Set-Content -LiteralPath "$corePom.sha256" -Value $localPomSha256 -NoNewline

$mavenContainer = "idax-open-core-local-maven"
$existingContainer = docker ps -a --filter "name=^/$mavenContainer$" --format "{{.Names}}"
if ($existingContainer) { docker rm -f $mavenContainer | Out-Null }
docker run -d --name $mavenContainer -p 127.0.0.1:8766:80 `
    --mount "type=bind,source=$mavenRoot,target=/usr/share/nginx/html,readonly" nginx:1.27-alpine | Out-Null
if ($LASTEXITCODE -ne 0) { throw "Could not start the local Core Maven repository." }

Write-Output "Local secrets and the Core 0.3 Maven repository are ready (ignored by Git)."
