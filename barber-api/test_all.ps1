Write-Host "=== TEST 1: POST /api/services ==="
$body = @{name="Test Service";price=50.0;durationInMin=30} | ConvertTo-Json
try {
    $resp = Invoke-RestMethod -Uri "http://localhost:8080/api/services" -Method POST -Body $body -ContentType "application/json"
    Write-Host "SUCCESS: $($resp | ConvertTo-Json -Compress)"
} catch { Write-Host "FAIL: $($_.Exception.Message)" }

Write-Host ""
Write-Host "=== TEST 2: POST /api/auth/register ==="
$body = @{email="test2@test.com";password="pass";firstName="A";lastName="B";phone="123"} | ConvertTo-Json
try {
    $resp = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register" -Method POST -Body $body -ContentType "application/json"
    Write-Host "SUCCESS: $($resp | ConvertTo-Json -Compress)"
} catch { Write-Host "FAIL: $($_.Exception.Message)" }

Write-Host ""
Write-Host "=== TEST 3: POST /api/appointments ==="
$body = @{dateTime="2026-04-10T10:00:00";barber=@{id=2};client=@{id=4};service=@{id=1}} | ConvertTo-Json -Depth 3
try {
    $resp = Invoke-RestMethod -Uri "http://localhost:8080/api/appointments" -Method POST -Body $body -ContentType "application/json"
    Write-Host "SUCCESS: $($resp | ConvertTo-Json -Compress -Depth 5)"
} catch { Write-Host "FAIL: $($_.Exception.Message)" }

Write-Host ""
Write-Host "=== TEST 4: GET /api/appointments ==="
try {
    $resp = Invoke-RestMethod -Uri "http://localhost:8080/api/appointments" -Method GET -ContentType "application/json"
    $json = $resp | ConvertTo-Json -Depth 5
    Write-Host "SUCCESS: $json"
    if ($json -match "hibernateLazyInitializer") {
        Write-Host "WARNING: hibernateLazyInitializer found in response!"
    } else {
        Write-Host "OK: No hibernateLazyInitializer in response"
    }
} catch { Write-Host "FAIL: $($_.Exception.Message)" }

Write-Host ""
Write-Host "=== TEST 5: GET /api/services ==="
try {
    $resp = Invoke-RestMethod -Uri "http://localhost:8080/api/services" -Method GET -ContentType "application/json"
    Write-Host "SUCCESS: $($resp | ConvertTo-Json -Compress -Depth 3)"
} catch { Write-Host "FAIL: $($_.Exception.Message)" }

Write-Host ""
Write-Host "=== TEST 6: PUT /api/services/1 ==="
$body = @{name="Updated";price=70.0;durationInMin=50} | ConvertTo-Json
try {
    $resp = Invoke-RestMethod -Uri "http://localhost:8080/api/services/1" -Method PUT -Body $body -ContentType "application/json"
    Write-Host "SUCCESS: $($resp | ConvertTo-Json -Compress)"
} catch { Write-Host "FAIL: $($_.Exception.Message)" }

Write-Host ""
Write-Host "=== TEST 7: PATCH /api/appointments/{id}/status ==="
$body = @{status="CONFIRMED"} | ConvertTo-Json
try {
    $resp = Invoke-RestMethod -Uri "http://localhost:8080/api/appointments/1/status" -Method PATCH -Body $body -ContentType "application/json"
    Write-Host "SUCCESS: $($resp | ConvertTo-Json -Compress -Depth 5)"
} catch { Write-Host "FAIL: $($_.Exception.Message)" }

Write-Host ""
Write-Host "=== TEST 8: GET /api/users ==="
try {
    $resp = Invoke-RestMethod -Uri "http://localhost:8080/api/users" -Method GET -ContentType "application/json"
    Write-Host "SUCCESS: $($resp | ConvertTo-Json -Compress -Depth 3)"
} catch { Write-Host "FAIL: $($_.Exception.Message)" }

Write-Host ""
Write-Host "=== TEST 9: GET /api/users/barbers ==="
try {
    $resp = Invoke-RestMethod -Uri "http://localhost:8080/api/users/barbers" -Method GET -ContentType "application/json"
    Write-Host "SUCCESS: $($resp | ConvertTo-Json -Compress -Depth 3)"
} catch { Write-Host "FAIL: $($_.Exception.Message)" }

Write-Host ""
Write-Host "=== TEST 10: GET /api/users/me ==="
try {
    $resp = Invoke-RestMethod -Uri "http://localhost:8080/api/users/me" -Method GET -ContentType "application/json"
    Write-Host "SUCCESS: $($resp | ConvertTo-Json -Compress)"
} catch { Write-Host "FAIL: $($_.Exception.Message)" }

Write-Host ""
Write-Host "=== TEST 11: DELETE /api/services/3 ==="
try {
    $resp = Invoke-WebRequest -Uri "http://localhost:8080/api/services/3" -Method DELETE -ContentType "application/json"
    Write-Host "SUCCESS: Status $($resp.StatusCode)"
} catch { Write-Host "FAIL: $($_.Exception.Message)" }

Write-Host ""
Write-Host "=== TEST 12: POST /api/auth/login ==="
$body = @{email="test@test.com";password="pass"} | ConvertTo-Json
try {
    $resp = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method POST -Body $body -ContentType "application/json"
    Write-Host "SUCCESS: $($resp | ConvertTo-Json -Compress)"
} catch { Write-Host "FAIL: $($_.Exception.Message)" }
