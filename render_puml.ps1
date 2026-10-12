# PlantUML Renderer via Kroki.io POST API
$filePath = "img\uml_diagrams\plantuml_src\use_case_overall_v2.puml"
$outputPath = "img\uml_diagrams\use_case_overall_v2.png"

$source = Get-Content $filePath -Raw -Encoding UTF8

# Kroki accepts JSON POST with diagram_source
$body = @{
    diagram_source = $source
    diagram_type = "plantuml"
    output_format = "png"
} | ConvertTo-Json -Depth 3

Write-Host "Sending to Kroki.io..."

try {
    Invoke-RestMethod -Uri "https://kroki.io/" -Method POST -Body $body -ContentType "application/json" -OutFile $outputPath
    $fileInfo = Get-Item $outputPath
    Write-Host "SUCCESS! Saved to $outputPath ($($fileInfo.Length) bytes)"
} catch {
    Write-Host "Kroki JSON failed, trying raw POST..."
    try {
        $utf8Bytes = [System.Text.Encoding]::UTF8.GetBytes($source)
        Invoke-WebRequest -Uri "https://kroki.io/plantuml/png" -Method POST -Body $utf8Bytes -ContentType "text/plain" -OutFile $outputPath -UseBasicParsing
        $fileInfo = Get-Item $outputPath
        Write-Host "SUCCESS! Saved to $outputPath ($($fileInfo.Length) bytes)"
    } catch {
        Write-Host "Error: $_"
    }
}
