<#
.SYNOPSIS
  Toma una captura del telefono por adb y la deja lista para Google Play (recortada a 2:1).

.DESCRIPTION
  La pantalla del telefono (1080x2340, 2.17:1) es mas alargada de lo que Play acepta (el lado largo no puede pasar de
  2 veces el corto). El script quita la barra de estado y un margen (arriba) y un poco de la zona de gestos (abajo) y deja
  1080x2160, sin cortar el menu inferior de la app.
  Guarda fuera del repositorio. Usar una cuenta demo: las capturas se publican.

.EXAMPLE
  .\scripts\play\capturar.ps1 -Nombre 01_inicio
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$Nombre,
    [string]$Salida = (Join-Path $env:USERPROFILE 'Documents\UrbanBlade\_play_store_assets\capturas'),
    [string]$Entrada   # solo para pruebas: usa esta imagen en lugar de capturar del telefono
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
New-Item -ItemType Directory -Force $Salida | Out-Null

if ($Entrada) {
    $origen = $Entrada
} else {
    $adb = Get-Command adb -ErrorAction SilentlyContinue
    if (-not $adb) { throw 'No encuentro adb. Agrega platform-tools al PATH.' }
    $dispositivos = (& adb devices) | Select-String -Pattern "`tdevice$"
    if (-not $dispositivos) { throw 'No hay telefono conectado (adb devices esta vacio). Enchufalo y activa la depuracion USB.' }
    $origen = Join-Path ([IO.Path]::GetTempPath()) "ub_captura_$Nombre.png"
    # cmd evita que PowerShell 5 corrompa los bytes del PNG al redirigir
    cmd /c "adb exec-out screencap -p > `"$origen`""
    if (-not (Test-Path $origen) -or (Get-Item $origen).Length -lt 1000) { throw 'La captura salio vacia.' }
}

$img = [System.Drawing.Image]::FromFile($origen)
try {
    $w = $img.Width
    $h = $img.Height
    $exceso = $h - (2 * $w)
    if ($exceso -gt 0) {
        # Abajo se quita lo minimo (60 px en 2340) para no cortar las etiquetas del menu; el resto sale de arriba
        # (barra de estado de 80 px mas un poco de margen).
        $abajo = [Math]::Min($exceso, [int][Math]::Round($h * 0.026))
        $arriba = $exceso - $abajo
        $alto = 2 * $w
    } else {
        $arriba = 0
        $alto = $h
    }
    $dest = New-Object System.Drawing.Bitmap($w, $alto, [System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
    $g = [System.Drawing.Graphics]::FromImage($dest)
    try {
        $rDest = New-Object System.Drawing.Rectangle(0, 0, $w, $alto)
        $rOrig = New-Object System.Drawing.Rectangle(0, $arriba, $w, $alto)
        $g.DrawImage($img, $rDest, $rOrig, [System.Drawing.GraphicsUnit]::Pixel)
    } finally { $g.Dispose() }
    $archivo = Join-Path $Salida "$Nombre.png"
    $dest.Save($archivo, [System.Drawing.Imaging.ImageFormat]::Png)
    $dest.Dispose()
} finally { $img.Dispose() }

$res = [System.Drawing.Image]::FromFile($archivo)
$ok = ($res.Width -ge 320) -and ($res.Height -ge 320) -and ([Math]::Max($res.Width, $res.Height) -le 2 * [Math]::Min($res.Width, $res.Height))
$valida = if ($ok) { 'si' } else { 'NO' }
Write-Host ("Guardada: {0}  ({1}x{2}, proporcion valida para Play: {3})" -f $archivo, $res.Width, $res.Height, $valida)
$res.Dispose()
