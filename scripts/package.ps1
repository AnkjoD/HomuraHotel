$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
& (Join-Path $PSScriptRoot 'build.ps1')
$manifest = [IO.Path]::GetTempFileName()
try {
    $libraries = Get-ChildItem -LiteralPath (Join-Path $projectRoot 'lib') -Filter '*.jar' | ForEach-Object { 'lib/' + $_.Name }
    $text = "Manifest-Version: 1.0`r`nMain-Class: main.Main`r`nClass-Path: " + ($libraries -join ' ') + "`r`n`r`n"
    [IO.File]::WriteAllText($manifest, $text, [Text.UTF8Encoding]::new($false))
    $jarArguments = @('cfm', (Join-Path $projectRoot 'HomuraHotel.jar'), $manifest)
    # Package runtime code and fonts only; never bundle local database credentials.
    foreach ($entry in @('main','ui','dao','database','entity','service','util','resources/font')) {
        $jarArguments += @('-C', (Join-Path $projectRoot 'bin'), $entry)
    }
    & jar @jarArguments
    if ($LASTEXITCODE -ne 0) { throw 'Packaging failed.' }
    Write-Host 'Created HomuraHotel.jar. Keep lib/, resources/, icon/ and img/ beside the JAR.'
} finally { Remove-Item -LiteralPath $manifest -ErrorAction SilentlyContinue }
