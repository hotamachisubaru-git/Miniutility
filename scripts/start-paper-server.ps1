param(
    [string] $ServerDirectory = $PSScriptRoot,
    [string] $ServerJar = 'paper-26.3-178.jar',
    [string] $JavaPath = 'C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot\bin\java.exe',
    [ValidateRange(1, 256)]
    [int] $MinimumMemoryGB = 2,
    [ValidateRange(1, 256)]
    [int] $MaximumMemoryGB = 4,
    [switch] $CheckOnly
)

$ErrorActionPreference = 'Stop'
if ($MinimumMemoryGB -gt $MaximumMemoryGB) {
    throw 'MinimumMemoryGB must not exceed MaximumMemoryGB.'
}
$resolvedServerDirectory = (Resolve-Path -LiteralPath $ServerDirectory).Path
$jarPath = Join-Path $resolvedServerDirectory $ServerJar
if (!(Test-Path -LiteralPath $jarPath -PathType Leaf)) {
    throw "Server jar not found: $jarPath"
}
if (!(Test-Path -LiteralPath $JavaPath -PathType Leaf)) {
    $JavaPath = (Get-Command java.exe -ErrorAction Stop).Source
}
$versionInfo = [System.Diagnostics.ProcessStartInfo]::new()
$versionInfo.FileName = $JavaPath
$versionInfo.Arguments = '-version'
$versionInfo.UseShellExecute = $false
$versionInfo.CreateNoWindow = $true
$versionInfo.RedirectStandardError = $true
$versionInfo.RedirectStandardOutput = $true
$versionProcess = [System.Diagnostics.Process]::Start($versionInfo)
try {
    $versionText = $versionProcess.StandardError.ReadToEnd() + $versionProcess.StandardOutput.ReadToEnd()
    $versionProcess.WaitForExit()
    if ($versionProcess.ExitCode -ne 0 -or $versionText -notmatch 'version\s+"(\d+)') {
        throw 'Unable to determine the Java version.'
    }
    if ([int] $Matches[1] -lt 25) {
        throw 'Paper 26.3 requires Java 25 or later.'
    }
} finally {
    $versionProcess.Dispose()
}
$eulaPath = Join-Path $resolvedServerDirectory 'eula.txt'
$eulaAccepted = (Test-Path -LiteralPath $eulaPath) -and
    ((Get-Content -LiteralPath $eulaPath) -match '^\s*eula\s*=\s*true\s*$').Count -gt 0
Write-Host "Server directory: $resolvedServerDirectory"
Write-Host "Java: $JavaPath"
Write-Host ("Memory: {0}G - {1}G" -f $MinimumMemoryGB, $MaximumMemoryGB)
Write-Host "EULA accepted: $eulaAccepted"
if ($CheckOnly) {
    return
}
if (!$eulaAccepted) {
    throw 'Read https://www.minecraft.net/eula and set eula=true in eula.txt if you agree.'
}
Push-Location -LiteralPath $resolvedServerDirectory
try {
    & $JavaPath '--enable-native-access=ALL-UNNAMED' ("-Xms{0}G" -f $MinimumMemoryGB) ("-Xmx{0}G" -f $MaximumMemoryGB) -jar $ServerJar nogui
    if ($LASTEXITCODE -ne 0) {
        throw "Paper exited with code $LASTEXITCODE. See logs/latest.log."
    }
} finally {
    Pop-Location
}
