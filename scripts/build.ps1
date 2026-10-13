param([string]$OutputDirectory = "bin", [switch]$Test)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
if (-not [IO.Path]::IsPathRooted($OutputDirectory)) { $OutputDirectory = Join-Path $projectRoot $OutputDirectory }
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
$compilerVersion = & javac -version 2>&1
if ($LASTEXITCODE -ne 0 -or "$compilerVersion" -notmatch 'javac (\d+)' -or [int]$Matches[1] -lt 21) {
    throw 'JDK 21 or newer is required.'
}
$argumentFile = [IO.Path]::GetTempFileName()
try {
    $sources = Get-ChildItem -LiteralPath (Join-Path $projectRoot 'src') -Recurse -Filter '*.java' |
        ForEach-Object { '"' + $_.FullName.Replace('\', '/') + '"' }
    [IO.File]::WriteAllLines($argumentFile, [string[]]$sources, [Text.UTF8Encoding]::new($false))
    & javac -proc:none -encoding UTF-8 -d $OutputDirectory -cp "$projectRoot/lib/*" "@$argumentFile"
    if ($LASTEXITCODE -ne 0) { throw 'Compilation failed.' }
    $resourceDirectory = Join-Path $OutputDirectory 'resources'
    New-Item -ItemType Directory -Force -Path $resourceDirectory | Out-Null
    Copy-Item -LiteralPath (Join-Path $projectRoot 'resources/font') -Destination $resourceDirectory -Recurse -Force
    Write-Host 'Build completed.'
} finally { Remove-Item -LiteralPath $argumentFile -ErrorAction SilentlyContinue }
if ($Test) {
    # Keep test preferences and login state out of the real application's files.
    $testDirectory = Join-Path $env:TEMP ('homura-tests-' + [Guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $testDirectory | Out-Null
    Push-Location -LiteralPath $testDirectory
    try {
        foreach ($testClass in @('test.HomuraSmokeTest', 'test.HomuraBusinessTest', 'test.HomuraThemeRegressionTest')) {
            & java '-Dfile.encoding=UTF-8' -ea -cp "$OutputDirectory;$projectRoot/lib/*" $testClass
            if ($LASTEXITCODE -ne 0) { throw "Test failed: $testClass" }
        }
    } finally { Pop-Location }
}
