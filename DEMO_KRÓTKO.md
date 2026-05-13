# JWT - DEMO DLA WYKŁADOWCY (KRÓTKO)

## TERMINAL 1 - Uruchom serwer (raz, na początek)

```powershell
cd C:\Users\kondz1o\Desktop\Barber\Barber
.\mvnw.cmd spring-boot:run
```

Czekaj aż pojawi się:
```
Tomcat started on port(s): 8080
```

---

## TERMINAL 2 - Wklej poniżej (całość naraz)

```powershell
# 1. Login KLIENT → dostaje token
$klientResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/auth/login" -Method POST -Headers @{"Content-Type"="application/json"} -Body '{"username":"klient1","password":"pass"}' -ErrorAction SilentlyContinue
$klientToken = ($klientResponse.Content | ConvertFrom-Json).token

Write-Output "=== KLIENT TOKEN ==="
Write-Output $klientToken
Write-Output ""

# 2. Weryfikuj token (pokaż username, role, datę wygaśnięcia)
Write-Output "=== ZAWARTOŚĆ TOKENU ==="
$verify = Invoke-WebRequest -Uri "http://localhost:8080/api/demo/verify-token" -Method GET -Headers @{"Content-Type"="application/json"; "Authorization"="Bearer $klientToken"} -ErrorAction SilentlyContinue
$verify.Content | ConvertFrom-Json | ConvertTo-Json

Write-Output ""

# 3. Login BARBER → dostaje token
$barberResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/auth/login" -Method POST -Headers @{"Content-Type"="application/json"} -Body '{"username":"barber1","password":"pass"}' -ErrorAction SilentlyContinue
$barberToken = ($barberResponse.Content | ConvertFrom-Json).token

Write-Output "=== BARBER TOKEN ==="
Write-Output $barberToken
Write-Output ""

# 4. BARBER dostęp do /api/tasks → DZIAŁA (200)
Write-Output "=== BARBER DOSTĘP DO /api/tasks ==="
$barberAccess = Invoke-WebRequest -Uri "http://localhost:8080/api/tasks" -Method GET -Headers @{"Content-Type"="application/json"; "Authorization"="Bearer $barberToken"} -ErrorAction SilentlyContinue
Write-Output "Status: $($barberAccess.StatusCode) ✓ OK"
Write-Output ""

# 5. KLIENT dostęp do /api/tasks → ZABRONIONE (403)
Write-Output "=== KLIENT DOSTĘP DO /api/tasks ==="
$klientAccess = Invoke-WebRequest -Uri "http://localhost:8080/api/tasks" -Method GET -Headers @{"Content-Type"="application/json"; "Authorization"="Bearer $klientToken"} -ErrorAction SilentlyContinue -FollowRelLink
Write-Output "Status: $($klientAccess.StatusCode) ✗ FORBIDDEN (bo rola KLIENT nie ma dostępu)"
Write-Output ""

# 6. Zmodyfikowany token → BŁĄD
Write-Output "=== ZMODYFIKOWANY TOKEN (FAŁSZYWY) ==="
$badToken = "eyJhbGciOiJIUzM4NCJ9.XXXX.cawomvnMz6EwlLF7pD1FTgGp3PEm5iyQ19Cvn4zRr4623VNSlDEiYFYn30jEatVB"
$badAccess = Invoke-WebRequest -Uri "http://localhost:8080/api/demo/verify-token" -Method GET -Headers @{"Content-Type"="application/json"; "Authorization"="Bearer $badToken"} -ErrorAction SilentlyContinue
Write-Output "Status: $($badAccess.StatusCode) ✗ ERROR"
Write-Output $badAccess.Content
```

---

## CO SIĘ DZIEJE (wyjaśnienie dla wykładowcy)

| Co | Wyjaśnienie |
|----|---|
| **Login** | Wysyłasz username + password → serwer zwraca **token JWT** |
| **Token JWT** | Zawiera username, role, datę wygaśnięcia, podpis cyfrowy |
| **Wysyłanie żądania** | Tokena wysyłasz w nagłówku `Authorization: Bearer ...` |
| **Serwer weryfikuje** | Sprawdza podpis tokenu (czy nikt go nie zmienił) + czy nie wygasł |
| **Rola w tokenie** | Na podstawie roli serwer pozwala lub zabrania dostępu (200 vs 403) |
| **Błąd tokenu** | Jeśli zmienisz 1 znak w tokenie = błąd weryfikacji |

---

## KONTA

```
KLIENT:  klient1 / pass
BARBER:  barber1 / pass
```

---

## GDZIE SĄ BŁĘDY JWT (logi serwera w TERMINAL 1)

Po wykonaniu poleceń, zobacz TERMINAL 1 i szukaj w logach:

- `Invalid JWT token: ...` - jeśli zmienisz token
- `JWT token is expired: ...` - jeśli token wygasł
- `JWT claims string is empty: ...` - jeśli brak tokenu

---

## KOD (do pokazania)

### Generowanie tokenu
[JwtUtils.java](src/main/java/com/example/barber/security/JwtUtils.java#L25):
```java
return Jwts.builder()
    .subject(username)              // kim jest użytkownik
    .claim("roles", roles)          // ← ROLE w tokenie!
    .expiration(new Date(...))      // kiedy wygaśnie
    .signWith(getSigningKey())      // ← podpis cyfrowy
    .compact();
```

### Weryfikacja tokenu
[JwtUtils.java](src/main/java/com/example/barber/security/JwtUtils.java#L101):
```java
Jwts.parser()
    .verifyWith(getSigningKey())    // sprawdź podpis
    .build()
    .parseSignedClaims(authToken);  // sprawdź termin ważności
```

### Kontrola dostępu
[SecurityConfig.java](src/main/java/com/example/barber/config/SecurityConfig.java#L38-L39):
```java
.requestMatchers("/api/tasks/**").hasAnyRole("ADMIN", "MANAGER", "BARBER")
// ← tylko te role mogą tutaj
```
