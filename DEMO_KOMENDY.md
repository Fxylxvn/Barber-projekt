# JWT - DEMO (KOMENDY)

## TERMINAL 1 - Serwer

```powershell
cd C:\Users\kondz1o\Desktop\Barber\Barber
.\mvnw.cmd spring-boot:run
```

Czekaj: `Tomcat started on port(s): 8080`

---

## TERMINAL 2 - Demo

```powershell
# Login KLIENT → token
$klientResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/auth/login" -Method POST -Headers @{"Content-Type"="application/json"} -Body '{"username":"klient1","password":"pass"}' -ErrorAction SilentlyContinue
$klientToken = ($klientResponse.Content | ConvertFrom-Json).token
Write-Output "Token: $klientToken"

# Weryfikuj token (pokaż role, username, datę)
$verify = Invoke-WebRequest -Uri "http://localhost:8080/api/demo/verify-token" -Method GET -Headers @{"Content-Type"="application/json"; "Authorization"="Bearer $klientToken"} -ErrorAction SilentlyContinue
$verify.Content | ConvertFrom-Json | ConvertTo-Json

# Login BARBER → token
$barberResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/auth/login" -Method POST -Headers @{"Content-Type"="application/json"} -Body '{"username":"barber1","password":"pass"}' -ErrorAction SilentlyContinue
$barberToken = ($barberResponse.Content | ConvertFrom-Json).token
Write-Output "Token: $barberToken"

# BARBER dostęp do /api/tasks (200 OK)
$barberAccess = Invoke-WebRequest -Uri "http://localhost:8080/api/tasks" -Method GET -Headers @{"Content-Type"="application/json"; "Authorization"="Bearer $barberToken"} -ErrorAction SilentlyContinue
Write-Output "BARBER /api/tasks: $($barberAccess.StatusCode)"

# KLIENT dostęp do /api/tasks (403 FORBIDDEN - brak uprawnień)
$klientAccess = Invoke-WebRequest -Uri "http://localhost:8080/api/tasks" -Method GET -Headers @{"Content-Type"="application/json"; "Authorization"="Bearer $klientToken"} -ErrorAction SilentlyContinue -FollowRelLink
Write-Output "KLIENT /api/tasks: $($klientAccess.StatusCode)"

# Zmodyfikowany token → ERROR
$badToken = "eyJhbGciOiJIUzM4NCJ9.XXXX.cawomvnMz6EwlLF7pD1FTgGp3PEm5iyQ19Cvn4zRr4623VNSlDEiYFYn30jEatVB"
$badAccess = Invoke-WebRequest -Uri "http://localhost:8080/api/demo/verify-token" -Method GET -Headers @{"Content-Type"="application/json"; "Authorization"="Bearer $badToken"} -ErrorAction SilentlyContinue
Write-Output "Fałszywy token: $($badAccess.StatusCode)"
Write-Output $badAccess.Content
```

---

## KONTA

```
klient1 / pass
barber1 / pass
```

---

## BŁĘDY JWT (TERMINAL 1 - logi serwera)

- `Invalid JWT token` - token zmieniony/fałszywy
- `JWT token is expired` - token wygasł
- `JWT claims string is empty` - brak tokenu
