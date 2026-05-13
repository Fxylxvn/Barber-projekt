# JWT - Demonstracja Mechanizmu dla Wykładowcy

## Przygotowanie

### Krok 1: Uruchom serwer

Otwórz PowerShell w folderze `C:\Users\kondz1o\Desktop\Barber\Barber` i wpisz:

```powershell
.\mvnw.cmd spring-boot:run
```

Czekaj aż zobaczysz w logach:
```
Tomcat started on port(s): 8080 (http)
```

---

## Demonstracja JWT

### Krok 2: Zaloguj się i uzyskaj token

Otwórz **nowe okno PowerShell** (nie zamykaj poprzedniego!) i wpisz:

```powershell
$response = Invoke-WebRequest -Uri "http://localhost:8080/api/auth/login" `
  -Method POST `
  -Headers @{"Content-Type"="application/json"} `
  -Body '{"username":"klient1","password":"pass"}' `
  -ErrorAction SilentlyContinue

$response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10
```

**Zobaczysz:**
```json
{
  "token": "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJrbGllbnQxIiwicm9sZXMiOlsiUk9MRV9LTElFTlQiXSwiaWF0IjoxNzc4MTg5NzA5LCJleHAiOjE3NzgyNzYxMDl9.cawomvnMz6EwlLF7pD1FTgGp3PEm5iyQ19Cvn4zRr4623VNSlDEiYFYn30jEatVB",
  "username": "klient1",
  "role": "KLIENT"
}
```

**Co to jest?**
- `token` - to JWT, trzyczęściowy string z punktami (.)
- `username` - zalogowany użytkownik
- `role` - rola w systemie

---

### Krok 3: Pokaż zawartość tokenu (bez dekodowania)

Token JWT ma trzy części rozdzielone `.`:

1. **HEADER** (część 1) - `eyJhbGciOiJIUzM4NCJ9` → zawiera typ algorytmu
2. **PAYLOAD** (część 2) - `eyJzdWIiOiJrbGllbnQxIiwicm9sZXMiOlsiUk9MRV9LTElFTlQiXSwiaWF0IjoxNzc4MTg5NzA5LCJleHAiOjE3NzgyNzYxMDl9` → zawiera dane (username, roles, daty)
3. **SIGNATURE** (część 3) - `cawomvnMz6EwlLF7pD1FTgGp3PEm5iyQ19Cvn4zRr4623VNSlDEiYFYn30jEatVB` → podpis cyfrowy (gwarancja niezmienności)

**Wniosek:** Token zawiera role użytkownika wewnątrz! Nie trzeba pytać bazy danych.

---

### Krok 4: Weryfikuj token i pokaż role

Skopiuj token z kroku 2 i wpisz w PowerShell:

```powershell
$token = "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJrbGllbnQxIiwicm9sZXMiOlsiUk9MRV9LTElFTlQiXSwiaWF0IjoxNzc4MTg5NzA5LCJleHAiOjE3NzgyNzYxMDl9.cawomvnMz6EwlLF7pD1FTgGp3PEm5iyQ19Cvn4zRr4623VNSlDEiYFYn30jEatVB"

$response = Invoke-WebRequest -Uri "http://localhost:8080/api/demo/verify-token" `
  -Method GET `
  -Headers @{
    "Content-Type" = "application/json"
    "Authorization" = "Bearer $token"
  } `
  -ErrorAction SilentlyContinue

$response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10
```

**Zobaczysz:**
```json
{
  "message": "Token jest poprawny",
  "username": "klient1",
  "roles": ["ROLE_KLIENT"],
  "expiration": "2026-05-08T23:21:49.000+0000",
  "authenticated_user": "klient1",
  "authorities": ["ROLE_KLIENT"]
}
```

**Co to dowodzi?**
- Serwer zweryfikował token (podpis + data wygaśnięcia)
- Wyciągnął z tokenu rolę: `ROLE_KLIENT`
- Ustawił kontekst bezpieczeństwa

---

### Krok 5: Pokaż błąd - token wygasły lub niepoprawny

Spróbuj wysłać GET bez tokena:

```powershell
$response = Invoke-WebRequest -Uri "http://localhost:8080/api/demo/verify-token" `
  -Method GET `
  -Headers @{"Content-Type"="application/json"} `
  -ErrorAction SilentlyContinue

$response.StatusCode
$response.Content
```

**Zobaczysz:** 400 + `Brak poprawnego nagłówka Authorization`

---

### Krok 6: Pokaż błąd - zmodyfikowany token

Zmień jeden znak w tokenie i spróbuj:

```powershell
$badToken = "eyJhbGciOiJIUzM4NCJ9.XXXX.cawomvnMz6EwlLF7pD1FTgGp3PEm5iyQ19Cvn4zRr4623VNSlDEiYFYn30jEatVB"

$response = Invoke-WebRequest -Uri "http://localhost:8080/api/demo/verify-token" `
  -Method GET `
  -Headers @{
    "Content-Type" = "application/json"
    "Authorization" = "Bearer $badToken"
  } `
  -ErrorAction SilentlyContinue

$response.StatusCode
$response.Content
```

**Zobaczysz:** 400 + `Błąd weryfikacji tokenu: ...`

**Wniosek:** Podpis chroniony przed modyfikacją!

---

### Krok 7: Pokaż dostęp - rola BARBER vs KLIENT

Zaloguj się jako BARBER:

```powershell
$response = Invoke-WebRequest -Uri "http://localhost:8080/api/auth/login" `
  -Method POST `
  -Headers @{"Content-Type"="application/json"} `
  -Body '{"username":"barber1","password":"pass"}' `
  -ErrorAction SilentlyContinue

$barberResponse = $response.Content | ConvertFrom-Json
$barberToken = $barberResponse.token

Write-Output "BARBER Token:"
$barberResponse | ConvertTo-Json
```

Teraz dostęp do endpointu `/api/tasks` (tylko dla BARBER, ADMIN, MANAGER):

```powershell
$response = Invoke-WebRequest -Uri "http://localhost:8080/api/tasks" `
  -Method GET `
  -Headers @{
    "Content-Type" = "application/json"
    "Authorization" = "Bearer $barberToken"
  } `
  -ErrorAction SilentlyContinue

Write-Output "Status: $($response.StatusCode)"
Write-Output "Response:"
$response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10
```

**Pokaż dla porównania - KLIENT spróbuje dostępu (403 Forbidden):**

```powershell
$response = Invoke-WebRequest -Uri "http://localhost:8080/api/tasks" `
  -Method GET `
  -Headers @{
    "Content-Type" = "application/json"
    "Authorization" = "Bearer $token"  # token z kroku 2 (KLIENT)
  } `
  -ErrorAction SilentlyContinue -FollowRelLink

Write-Output "Status: $($response.StatusCode)"
```

---

## Podsumowanie dla Wykładowcy

| Etap | Co się dzieje | Gdzie | Kod |
|------|---------------|-------|-----|
| **1. Login** | Użytkownik i hasło → **Token JWT** | `/api/auth/login` | [AuthRestController.java](src/main/java/com/example/barber/controller/api/AuthRestController.java#L30) |
| **2. Generowanie** | Token zawiera username + role | `JwtUtils.generateJwtToken()` | [JwtUtils.java](src/main/java/com/example/barber/security/JwtUtils.java#L25) |
| **3. Każde żądanie** | Token w nagłówku `Authorization: Bearer ...` | `AuthTokenFilter` | [AuthTokenFilter.java](src/main/java/com/example/barber/security/AuthTokenFilter.java#L44) |
| **4. Weryfikacja** | Serwer sprawdza podpis i datę wygaśnięcia | `validateJwtToken()` | [JwtUtils.java](src/main/java/com/example/barber/security/JwtUtils.java#L101) |
| **5. Autoryzacja** | Na podstawie role z tokenu - pozwól/zabroń dostęp | `SecurityConfig` | [SecurityConfig.java](src/main/java/com/example/barber/config/SecurityConfig.java#L38) |
| **6. Response** | `200 OK` lub `403 Forbidden` zależnie od roli | `/api/tasks`, `/api/reservations` | [TaskRestController.java](src/main/java/com/example/barber/controller/api/TaskRestController.java) |

---

## Konta do testowania

```
KLIENT:  username=klient1, password=pass
KLIENT:  username=klient2, password=pass
KLIENT:  username=klient3, password=pass

BARBER:  username=barber1, password=pass
BARBER:  username=barber2, password=pass
BARBER:  username=barber3, password=pass
BARBER:  username=barber4, password=pass
```

Wszystkie konta mają hasło: `pass`

---

## Błędy JWT do pokazania

| Błąd | Jak go wyświetlić | Gdzie se pojawia |
|------|-------------------|------------------|
| **Invalid JWT token** | Zmień jeden znak w tokenie | Logi serwera + `verify-token` 400 |
| **JWT token is expired** | Czekaj 24h lub zmodyfikuj `barber.app.jwtExpirationMs` | Logi serwera + `verify-token` 400 |
| **JWT claims string is empty** | Wyślij pusty `Authorization: Bearer` | Logi serwera + `verify-token` 400 |
| **Method Not Allowed (405)** | Zaloguj się bez headera `Content-Type` | Odpowiedź HTTP |
| **Forbidden (403)** | KLIENT próbuje dostępu do `/api/tasks` | Odpowiedź HTTP |

---

## Kod do pokazania wykładowcy

### 1. Generowanie tokenu - [JwtUtils.java](src/main/java/com/example/barber/security/JwtUtils.java#L25)

```java
return Jwts.builder()
    .subject((userPrincipal.getUsername()))
    .claim("roles", roles)                    // ← ROLE zawarte w tokenie!
    .issuedAt(new Date())
    .expiration(new Date((new Date()).getTime() + jwtExpirationMs))
    .signWith(getSigningKey())               // ← Podpis cyfrowy
    .compact();
```

### 2. Weryfikacja tokenu - [JwtUtils.java](src/main/java/com/example/barber/security/JwtUtils.java#L101)

```java
public boolean validateJwtToken(String authToken) {
    try {
        Jwts.parser()
            .verifyWith((javax.crypto.SecretKey) getSigningKey())
            .build()
            .parseSignedClaims(authToken);    // ← Sprawdza podpis + termin
        return true;
    } catch (ExpiredJwtException e) {
        logger.error("JWT token is expired: {}", e.getMessage());
    } catch (MalformedJwtException e) {
        logger.error("Invalid JWT token: {}", e.getMessage());
    }
    return false;
}
```

### 3. Filtr na każde żądanie - [AuthTokenFilter.java](src/main/java/com/example/barber/security/AuthTokenFilter.java#L44)

```java
if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
    String username = jwtUtils.getUserNameFromJwtToken(jwt);
    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
    
    // ← Ustawiamy autentykację w kontekście Spring Security
    SecurityContextHolder.getContext().setAuthentication(authentication);
}
```

### 4. Kontrola dostępu - [SecurityConfig.java](src/main/java/com/example/barber/config/SecurityConfig.java#L38)

```java
.requestMatchers("/api/tasks/**").hasAnyRole("ADMIN", "MANAGER", "BARBER")
// ← Tylko te role mogą!
```

---

## Gotowe komendy do skopiowania i wklejenia

Skopiuj cały blok poniżej i wklej w PowerShell:

```powershell
# Krok 1: Login KLIENT
$klientResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/auth/login" -Method POST -Headers @{"Content-Type"="application/json"} -Body '{"username":"klient1","password":"pass"}' -ErrorAction SilentlyContinue
$klientToken = ($klientResponse.Content | ConvertFrom-Json).token
Write-Output "=== KLIENT1 Token ==="
$klientResponse.Content | ConvertFrom-Json | ConvertTo-Json

# Krok 2: Verify token
Write-Output "`n=== Weryfikacja tokenu KLIENT ==="
$verifyResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/demo/verify-token" -Method GET -Headers @{"Content-Type"="application/json"; "Authorization"="Bearer $klientToken"} -ErrorAction SilentlyContinue
$verifyResponse.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10

# Krok 3: Login BARBER
$barberResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/auth/login" -Method POST -Headers @{"Content-Type"="application/json"} -Body '{"username":"barber1","password":"pass"}' -ErrorAction SilentlyContinue
$barberToken = ($barberResponse.Content | ConvertFrom-Json).token
Write-Output "`n=== BARBER1 Token ==="
$barberResponse.Content | ConvertFrom-Json | ConvertTo-Json

# Krok 4: BARBER dostęp do /api/tasks (200 OK)
Write-Output "`n=== BARBER dostęp do /api/tasks (powinno działać) ==="
$barberTasksResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/tasks" -Method GET -Headers @{"Content-Type"="application/json"; "Authorization"="Bearer $barberToken"} -ErrorAction SilentlyContinue
Write-Output "Status: $($barberTasksResponse.StatusCode)"

# Krok 5: KLIENT dostęp do /api/tasks (403 Forbidden)
Write-Output "`n=== KLIENT dostęp do /api/tasks (powinno być zabronione) ==="
$klientTasksResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/tasks" -Method GET -Headers @{"Content-Type"="application/json"; "Authorization"="Bearer $klientToken"} -ErrorAction SilentlyContinue -FollowRelLink
Write-Output "Status: $($klientTasksResponse.StatusCode)"

# Krok 6: Zmodyfikowany token (powinien być błąd)
Write-Output "`n=== Zmodyfikowany token (powinien być błąd) ==="
$badToken = "eyJhbGciOiJIUzM4NCJ9.XXXX.cawomvnMz6EwlLF7pD1FTgGp3PEm5iyQ19Cvn4zRr4623VNSlDEiYFYn30jEatVB"
$badResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/demo/verify-token" -Method GET -Headers @{"Content-Type"="application/json"; "Authorization"="Bearer $badToken"} -ErrorAction SilentlyContinue
Write-Output "Status: $($badResponse.StatusCode)"
$badResponse.Content
```

---

## Linki do kodu

- [JwtUtils.java](src/main/java/com/example/barber/security/JwtUtils.java) - generowanie i weryfikacja
- [AuthTokenFilter.java](src/main/java/com/example/barber/security/AuthTokenFilter.java) - filtr na każde żądanie
- [AuthRestController.java](src/main/java/com/example/barber/controller/api/AuthRestController.java) - endpoint login
- [TokenDemoController.java](src/main/java/com/example/barber/controller/api/TokenDemoController.java) - endpoint verify-token
- [SecurityConfig.java](src/main/java/com/example/barber/config/SecurityConfig.java) - kontrola dostępu na podstawie ról
