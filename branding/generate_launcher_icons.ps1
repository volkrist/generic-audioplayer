# Generates launcher icon PNGs for every mipmap density from branding/app_icon.png.
# Run from the repository root: powershell -ExecutionPolicy Bypass -File .\branding\generate_launcher_icons.ps1
Add-Type -AssemblyName System.Drawing

$ErrorActionPreference = 'Stop'
$src = Join-Path $PSScriptRoot 'app_icon.png'
if (-not (Test-Path $src)) { throw "Source not found: $src" }

$resDir = Resolve-Path (Join-Path $PSScriptRoot '..\app\src\main\res')

# Launcher icon (square) and round variants share the same bitmap - Android masks the round one.
$launcherSizes = @{
    'mipmap-mdpi'    = 48
    'mipmap-hdpi'    = 72
    'mipmap-xhdpi'   = 96
    'mipmap-xxhdpi'  = 144
    'mipmap-xxxhdpi' = 192
}
# Adaptive icon foreground is 108dp - larger canvas so Android can crop safely.
$foregroundSizes = @{
    'mipmap-mdpi'    = 108
    'mipmap-hdpi'    = 162
    'mipmap-xhdpi'   = 216
    'mipmap-xxhdpi'  = 324
    'mipmap-xxxhdpi' = 432
}

$original = [System.Drawing.Image]::FromFile($src)
try {
    function Save-Scaled([System.Drawing.Image]$img, [string]$out, [int]$size) {
        $bmp = New-Object System.Drawing.Bitmap $size, $size
        $g = [System.Drawing.Graphics]::FromImage($bmp)
        $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
        $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
        $g.DrawImage($img, 0, 0, $size, $size)
        $g.Dispose()
        $dir = Split-Path -Parent $out
        if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
        $bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
        $bmp.Dispose()
        Write-Host "wrote $out ($size x $size)"
    }

    foreach ($entry in $launcherSizes.GetEnumerator()) {
        $folder = Join-Path $resDir $entry.Key
        Save-Scaled $original (Join-Path $folder 'ic_launcher.png')       $entry.Value
        Save-Scaled $original (Join-Path $folder 'ic_launcher_round.png') $entry.Value
    }
    foreach ($entry in $foregroundSizes.GetEnumerator()) {
        $folder = Join-Path $resDir $entry.Key
        Save-Scaled $original (Join-Path $folder 'ic_launcher_foreground.png') $entry.Value
    }
}
finally {
    $original.Dispose()
}
Write-Host "launcher icons regenerated"
