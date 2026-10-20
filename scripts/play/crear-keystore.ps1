<#
.SYNOPSIS
  Crea la llave de subida (keystore) para Google Play y deja sus datos en local.properties.

.DESCRIPTION
  - Pide la contrasena sin mostrarla y NO la pasa por la linea de comandos (usa una variable de entorno temporal).
  - Se niega a sobrescribir un keystore que ya exista y a crearlo dentro del repositorio.
  - Agrega UPLOAD_STORE_FILE / UPLOAD_STORE_PASSWORD / UPLOAD_KEY_ALIAS / UPLOAD_KEY_PASSWORD a local.properties
    (archivo que git ignora). Si ya hay claves UPLOAD_*, se detiene en vez de duplicarlas.
  - Imprime las huellas SHA-1 y SHA-256 (son publicas): la SHA-1 se registra en Firebase para probar el login con Google
    con un AAB firmado con esta llave.

  ATENCION: si pierdes este archivo o su contrasena no podras actualizar la app en Play. Respaldalo en 2 lugares.

.EXAMPLE
  .\scripts\play\crear-keystore.ps1
#>
[CmdletBinding()]
param(
    [string]$Destino = (Join-Path $env:USERPROFILE 'urbanblade-keys\urbanblade-upload.jks'),
    [string]$Alias = 'upload',
    [string]$Titular = 'UrbanBlade'
)

$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$props = Join-Path $repo 'local.properties'

function Find-Keytool {
    $candidatos = @()
    if ($env:JAVA_HOME) { $candidatos += (Join-Path $env:JAVA_HOME 'bin\keytool.exe') }
    $candidatos += 'C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe'
    foreach ($c in $candidatos) { if (Test-Path $c) { return $c } }
    $cmd = Get-Command keytool -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    throw 'No encuentro keytool. Instala Android Studio o define JAVA_HOME.'
}

if ($Destino.StartsWith($repo, [StringComparison]::OrdinalIgnoreCase)) {
    throw "El keystore no puede estar dentro del repositorio ($repo). Usa una carpeta fuera, por ejemplo $env:USERPROFILE\urbanblade-keys."
}
if (Test-Path $Destino) {
    throw "Ya existe $Destino. No lo sobrescribo: si es el de subida, solo agrega sus datos a local.properties a mano."
}
if ((Test-Path $props) -and (Select-String -Path $props -Pattern '^\s*UPLOAD_STORE_FILE\s*=' -Quiet)) {
    throw 'local.properties ya tiene UPLOAD_STORE_FILE. Revisa ese archivo antes de crear otra llave.'
}

$keytool = Find-Keytool
Write-Host "keytool: $keytool"
Write-Host "Se creara: $Destino  (alias '$Alias', RSA 2048, 10000 dias)"
Write-Host 'La contrasena debe tener al menos 8 caracteres y no usar \ = : ni espacios al inicio o al final.'

function Read-Secreto([string]$pregunta) {
    $s = Read-Host -AsSecureString $pregunta
    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($s)
    try { return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr) }
}

$pw = Read-Secreto 'Contrasena del keystore'
$pw2 = Read-Secreto 'Repite la contrasena'
if ($pw -ne $pw2) { throw 'Las contrasenas no coinciden.' }
if ($pw.Length -lt 8 -or $pw -match '[\\=:]' -or $pw -ne $pw.Trim()) { throw 'Contrasena invalida (ver reglas arriba).' }

$dir = Split-Path -Parent $Destino
if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Force $dir | Out-Null }

$env:UB_KS_PW = $pw
try {
    & $keytool -genkeypair -keystore $Destino -alias $Alias -keyalg RSA -keysize 2048 -validity 10000 `
        -storepass:env UB_KS_PW -keypass:env UB_KS_PW `
        -dname "CN=$Titular, OU=Proyecto academico, O=UrbanBlade, C=MX"
    if ($LASTEXITCODE -ne 0) { throw "keytool fallo (codigo $LASTEXITCODE)." }

    $huellas = & $keytool -list -v -keystore $Destino -alias $Alias -storepass:env UB_KS_PW
    $ruta = $Destino -replace '\\', '\\'
    Add-Content -Path $props -Value @(
        '',
        '# Llave de subida de Google Play (creada con scripts/play/crear-keystore.ps1). No subir a git.',
        "UPLOAD_STORE_FILE=$ruta",
        "UPLOAD_STORE_PASSWORD=$pw",
        "UPLOAD_KEY_ALIAS=$Alias",
        "UPLOAD_KEY_PASSWORD=$pw"
    )
}
finally {
    Remove-Item Env:\UB_KS_PW -ErrorAction SilentlyContinue
    $pw = $null
    $pw2 = $null
}

Write-Host ''
Write-Host "Keystore creado: $Destino" -ForegroundColor Green
Write-Host "Datos agregados a: $props"
Write-Host ''
Write-Host 'Huellas de la llave de subida (publicas; la SHA-1 va a Firebase):'
$huellas | Select-String -Pattern 'SHA1:|SHA256:' | ForEach-Object { '  ' + $_.Line.Trim() }
Write-Host ''
Write-Host 'AHORA: respalda el .jks y la contrasena en 2 lugares distintos (no en el repositorio).' -ForegroundColor Yellow
