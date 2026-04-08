$body = @{
    dateTime = "2026-04-10T10:00:00"
    barber = @{ id = 2 }
    client = @{ id = 4 }
    service = @{ id = 1 }
} | ConvertTo-Json -Depth 3

Write-Host "Request body:"
Write-Host $body
Write-Host ""

try {
    $resp = Invoke-RestMethod -Uri "http://localhost:8080/api/appointments" -Method POST -Body $body -ContentType "application/json"
    Write-Host "SUCCESS:"
    $resp | ConvertTo-Json -Depth 5
} catch {
    Write-Host "ERROR:"
    Write-Host $_.Exception.Message
    if ($_.ErrorDetails) {
        Write-Host "Details:"
        Write-Host $_.ErrorDetails.Message
    }
}
