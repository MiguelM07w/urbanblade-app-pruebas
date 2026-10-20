<#
.SYNOPSIS
  Verifica la configuracion de release y genera el AAB firmado para Google Play.

.DESCRIPTION
  Antes de compilar comprueba lo mismo que la guarda de Gradle (con mensajes mas claros) y despues ejecuta
  `gradle :app:bundleRelease`. Al terminar muestra ruta, tamano, SHA-256 del AAB y la huella del certificado que lo firmo.
  No imprime contrasenas.

.EXAMPLE
  .\scripts\play\generar-aab.ps1
#>
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$propsFile = Join-Path $repo 'local.properties'
if (-not (Test-Path $propsFile)) { throw "No existe $propsFile." }

# --- leer local.properties (clave=valor) ---
$p = @{}
Get-Content $propsFile | ForEach-Object {
    if ($_ -match '^\s*([A-Za-z0-9_.]+)\s*=\s*(.*)$') { $p[$Matches[1]] = $Matches[2].Trim() }
}

$faltan = New-Object System.Collections.Generic.List[string]
if (-not $p['RELEASE_API_BASE_URL'] -or $p['RELEASE_API_BASE_URL'] -notmatch '^https://' -or $p['RELEASE_API_BASE_URL'] -match 'PENDIENTE') {
    $faltan.Add('RELEASE_API_BASE_URL (debe ser https real, p. ej. https://api.urbanblade.com.mx/api/v1/)')
}
if (-not $p['GOOGLE_CLIENT_ID'] -or $p['GOOGLE_CLIENT_ID'] -match 'PENDIENTE') {
    $faltan.Add('GOOGLE_CLIENT_ID (Web Client ID de produccion)')
}
$stripe = $p['STRIPE_PUBLISHABLE_KEY']
$pruebaPermitida = ($p['ALLOW_TEST_PAYMENTS'] -eq 'true') -and $stripe -and ($stripe -match '^pk_test_') -and ($stripe -notmatch 'PENDIENTE')
if (-not $stripe -or ($stripe -notmatch '^pk_live_' -and -not $pruebaPermitida)) {
    $faltan.Add('STRIPE_PUBLISHABLE_KEY debe ser pk_live_ (Camino B). Para el prototipo academico (Camino A) pon una pk_test_ real y ALLOW_TEST_PAYMENTS=true en local.properties; ver docs/PUBLICAR_PLAY_STORE.md seccion 0.1')
}
if ($pruebaPermitida) {
    Write-Host 'AVISO: este AAB lleva Stripe en MODO DE PRUEBA (Camino A): no cobra dinero real. La ficha de Play debe decirlo.' -ForegroundColor Yellow
}
foreach ($k in 'UPLOAD_STORE_FILE', 'UPLOAD_STORE_PASSWORD', 'UPLOAD_KEY_ALIAS', 'UPLOAD_KEY_PASSWORD') {
    if (-not $p[$k]) { $faltan.Add("$k (ejecuta .\scripts\play\crear-keystore.ps1)") }
}
if ($p['UPLOAD_STORE_FILE']) {
    $ks = $p['UPLOAD_STORE_FILE'] -replace '\\\\', '\'
    if (-not (Test-Path $ks)) { $faltan.Add("el archivo de UPLOAD_STORE_FILE no existe: $ks") }
}
if ($faltan.Count -gt 0) {
    Write-Host 'No se puede generar el AAB todavia. Falta:' -ForegroundColor Red
    $faltan | ForEach-Object { Write-Host "  - $_" }
    exit 1
}

# --- version ---
$gradleApp = Get-Content (Join-Path $repo 'app\build.gradle.kts') -Raw
$vc = [regex]::Match($gradleApp, 'versionCode\s*=\s*(\d+)').Groups[1].Value
$vn = [regex]::Match($gradleApp, 'versionName\s*=\s*"([^"]+)"').Groups[1].Value
Write-Host "Version: $vn (versionCode $vc). Play rechaza subir un versionCode que ya uso." -ForegroundColor Cyan

# --- JAVA_HOME y Gradle ---
if (-not $env:JAVA_HOME) {
    $jbr = 'C:\Program Files\Android\Android Studio\jbr'
    if (Test-Path $jbr) { $env:JAVA_HOME = $jbr } else { throw 'Define JAVA_HOME o instala Android Studio.' }
}
$wrapper = Get-Content (Join-Path $repo 'gradle\wrapper\gradle-wrapper.properties') -Raw
$gv = [regex]::Match($wrapper, 'gradle-([0-9.]+)-bin').Groups[1].Value
$dists = Join-Path $env:USERPROFILE ".gradle\wrapper\dists\gradle-$gv-bin"
$gradle = $null
if (Test-Path $dists) {
    $gradle = Get-ChildItem $dists -Recurse -Filter gradle.bat -ErrorAction SilentlyContinue | Select-Object -First 1
}
if ($gradle) {
    $gradleExe = $gradle.FullName
} else {
    $g = Get-Command gradle -ErrorAction SilentlyContinue
    if (-not $g) { throw "No encuentro Gradle $gv. Abre el proyecto una vez en Android Studio para que lo descargue." }
    $gradleExe = $g.Source
}
Write-Host "Gradle $gv : $gradleExe"

Push-Location $repo
try {
    & $gradleExe --no-daemon :app:bundleRelease
    if ($LASTEXITCODE -ne 0) { throw "Gradle fallo (codigo $LASTEXITCODE)." }
}
finally { Pop-Location }

$aab = Join-Path $repo 'app\build\outputs\bundle\release\app-release.aab'
if (-not (Test-Path $aab)) { throw "No se genero $aab." }
$info = Get-Item $aab
$hash = (Get-FileHash $aab -Algorithm SHA256).Hash
Write-Host ''
Write-Host 'AAB generado' -ForegroundColor Green
Write-Host ("  Ruta   : {0}" -f $aab)
Write-Host ("  Tamano : {0:N1} MB" -f ($info.Length / 1MB))
Write-Host ("  SHA-256: {0}" -f $hash)

$keytool = Join-Path $env:JAVA_HOME 'bin\keytool.exe'
if (Test-Path $keytool) {
    Write-Host '  Certificado de firma:'
    & $keytool -printcert -jarfile $aab 2>$null | Select-String -Pattern 'SHA1:|SHA256:|Owner:' | ForEach-Object { '    ' + $_.Line.Trim() }
}
Write-Host ''
Write-Host 'Siguiente: Play Console -> Pruebas -> Prueba interna (o cerrada) -> Crear version -> subir este AAB.' -ForegroundColor Yellow
Write-Host 'Despues de la primera subida: registrar la SHA-1 de "Firma de la app" en Firebase (guia, seccion 3).'
