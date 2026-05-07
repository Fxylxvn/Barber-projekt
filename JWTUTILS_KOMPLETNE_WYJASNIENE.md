# 🔐 JwtUtils.java - Kompletne Wyjaśnienie Kodu

## 📋 Spis Treści

1. [Przegląd Klasy](#przegląd-klasy)
2. [Zmienne Konfiguracyjne](#zmienne-konfiguracyjne)
3. [Metoda: generateJwtToken()](#metoda-generatejwttoken)
4. [Metoda: getUserNameFromJwtToken()](#metoda-getusernamefromiwttoken)
5. [Metoda: validateJwtToken()](#metoda-validatejwttoken)
6. [Flowchart Całego Procesu](#flowchart)

---

## Przegląd Klasy

```
┌──────────────────────────────────────────────────────────────────┐
│ JwtUtils - GŁÓWNA KLASA DO PRACY Z TOKENAMI JWT                  │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│ Trzy główne obowiązki:                                           │
│                                                                   │
│ 1️⃣ generateJwtToken()      → Tworzy nowy token                   │
│    Wejście: zalogowany użytkownik                               │
│    Wyjście: token JWT                                           │
│                                                                   │
│ 2️⃣ getUserNameFromJwtToken() → Odczytuje username z tokena      │
│    Wejście: token JWT                                           │
│    Wyjście: "jan_kowalski"                                      │
│                                                                   │
│ 3️⃣ validateJwtToken()      → Sprawdza czy token jest OK         │
│    Wejście: token JWT                                           │
│    Wyjście: true (OK) lub false (błąd)                          │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

---

## Zmienne Konfiguracyjne

### 1. Logger

```java
private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);
```

**Co to jest:**
- Narzędzie do rejestrowania zdarzeń i błędów
- Tworzy logi które mogą być przejrzane później

**Przykład użycia:**
```java
logger.error("Invalid JWT token: {}", e.getMessage());
// → [ERROR] JwtUtils - Invalid JWT token: Token expired
```

---

### 2. jwtSecret - Tajny Klucz

```java
@Value("${barber.app.jwtSecret:myVerySecretKeyForBarberAppThatIsAtLeast32CharactersLong}")
private String jwtSecret;
```

**Co to jest:**
- Długi, tajny ciąg znaków (minimum 32 znaki)
- Używany do podpisywania i weryfikacji tokenów
- **TYLKO serwer go zna** - to jest sekret!

**Jak działa:**
```
Wygenerowanie tokena:
token = HASH(dane_tokena + jwtSecret)

Weryfikacja tokena:
podpis_serwera = HASH(dane_tokena + jwtSecret)
Jeśli podpis_serwera == podpis_z_tokena → OK
Jeśli podpis_serwera != podpis_z_tokena → ALARM! Token został zmieniony!
```

**Bezpieczeństwo:**
- Jeśli ktoś ma token i zna jwtSecret → może sfałszować token ❌
- Jeśli ktoś ma token ale NIE zna jwtSecret → nie może go zmienić ✅
- Dlatego **jwtSecret nigdy nie powinien być widoczny w kodzie**!

**Gdzie ustawić:**
```properties
# Plik: src/main/resources/application.properties
barber.app.jwtSecret=twojSuperTajnyKluczDlugi32ZnakowMinimum
```

---

### 3. jwtExpirationMs - Czas Wygaśnięcia

```java
@Value("${barber.app.jwtExpirationMs:86400000}")
private int jwtExpirationMs;
```

**Co to jest:**
- Liczba milisekund jak długo token będzie ważny
- 86400000 ms = 86400 s = 1440 min = 24 godziny

**Jak to wygląda:**

```
┌────────────────────────────────────────┐
│ Token WYGENEROWANY: 7 maja 20:00       │
│ Token WYGASA: 8 maja 20:00 (24h później)
└────────────────────────────────────────┘

TERAZ: 7 maja 20:30
└─ Token ma 30 minut - WAŻNY ✅

TERAZ: 8 maja 10:00
└─ Token ma 14 godzin - WAŻNY ✅

TERAZ: 8 maja 21:00
└─ Token ma 25 godzin - WYGASŁ ❌
   "Zaloguj się ponownie!"
```

**Bezpieczeństwo:**
- Krótszy czas (1h) = bezpieczniej
- Dłuższy czas (7 dni) = wygodniej dla użytkownika
- Rekomendacja: 1-24 godziny

---

## Metoda: generateJwtToken()

### Gdzie się wywołuje?
```
POST /api/auth/login
  ↓
AuthRestController.authenticateUser()
  ↓
jwtUtils.generateJwtToken(authentication)  ← TUTAJ
  ↓
Token JWT wrócony do klienta
```

### Krok po Kroku

```java
public String generateJwtToken(Authentication authentication) {
```

#### Krok 1: Wyodrębnienie UserDetails

```java
UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
```

**Co się dzieje:**
- `authentication` - obiekt Spring Security z zalogowanym użytkownikiem
- `.getPrincipal()` - pobierz główny obiekt (użytkownika)
- `(UserDetails)` - rzutowanie na typ UserDetails

**Zawiera:**
- Username: "jan_kowalski"
- Password: "haslo123" (zaszyfrowane)
- Authorities (role): ["ROLE_KLIENT"]
- Czy konto aktywne: true

---

#### Krok 2: Wyodrębnienie Roli

```java
java.util.List<String> roles = userPrincipal.getAuthorities().stream()
        .map(org.springframework.security.core.GrantedAuthority::getAuthority)
        .collect(java.util.stream.Collectors.toList());
```

**Co się tutaj dzieje:**

```
userPrincipal.getAuthorities()
└─ Zwraca: [GrantedAuthority("ROLE_KLIENT")]

.stream()
└─ Zamienia listę w stream do przetwarzania

.map(getAuthority())
└─ Dla każdego GrantedAuthority wyodrębnij tekst
└─ Przed: GrantedAuthority object
└─ Po: String "ROLE_KLIENT"

.collect(toList())
└─ Zbierz wszystko w listę
└─ Wynik: ["ROLE_KLIENT"]
```

**Rezultat:**
```java
roles = ["ROLE_KLIENT"]
```

---

#### Krok 3: Budowanie Tokena

```java
return Jwts.builder()
        .subject((userPrincipal.getUsername()))        // "jan_kowalski"
        .claim("roles", roles)                         // ["ROLE_KLIENT"]
        .issuedAt(new Date())                          // 7 maja 20:00
        .expiration(new Date(...))                     // 8 maja 20:00
        .signWith(getSigningKey())                     // PODPISZ
        .compact();                                     // SPAKUJ
```

**Część po Części:**

| Metoda | Co Robi | Rezultat w Tokenie |
|--------|---------|-------------------|
| `.subject()` | Wpisz username | `"sub": "jan_kowalski"` |
| `.claim("roles")` | Wpisz role | `"roles": ["ROLE_KLIENT"]` |
| `.issuedAt()` | Data wydania | `"iat": 1778179121` |
| `.expiration()` | Data wygaśnięcia | `"exp": 1778265521` |
| `.signWith()` | Podpisz kluczem | `SIGNATURE: Eq5Ej9C5...` |
| `.compact()` | Spakuj w string | Finałowy token |

**Finałowy Token:**
```
eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJqYW5fa293YWxza2kiLCJyb2xlcyI6WyJST0xFX0tMSUVOVCJdLCJpYXQiOjE3NzgxNzkxMjEsImV4cCI6MTc3ODI2NTUyMX0.Eq5Ej9C5PMZ7A2J-HPuZVKH_UxylVWKFmbi2xtcVxXscq_A0EWq2pCAOtYXJ3CFD
```

**To jest 3 części:**
1. **HEADER** (Base64): `eyJhbGciOiJIUzM4NCJ9`
2. **PAYLOAD** (Base64): `eyJzdWIiOiJqYW5fa293YWxza2kiLCJyb2xlcyI6WyJST0xFX0tMSUVOVCJdLCJpYXQiOjE3NzgxNzkxMjEsImV4cCI6MTc3ODI2NTUyMX0`
3. **SIGNATURE** (Hash): `Eq5Ej9C5PMZ7A2J-HPuZVKH_UxylVWKFmbi2xtcVxXscq_A0EWq2pCAOtYXJ3CFD`

---

## Metoda: getUserNameFromJwtToken()

### Gdzie się wywołuje?
```
GET /api/demo/verify-token (z tokenem w nagłówku)
  ↓
AuthTokenFilter.doFilterInternal()
  ↓
jwtUtils.getUserNameFromJwtToken(token)  ← TUTAJ
  ↓
Username: "jan_kowalski" (zwrócony)
```

### Krok po Kroku

```java
public String getUserNameFromJwtToken(String token) {
    return Jwts.parser()
            .verifyWith((javax.crypto.SecretKey) getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getSubject();
}
```

**Rozbicie:**

```
Jwts.parser()
└─ Utwórz parser (narzędzie do odczytywania)

.verifyWith(getSigningKey())
└─ Sprawdzenie: czy podpis jest poprawny?
└─ Jeśli token został zmieniony → rzuć wyjątek!

.build()
└─ Zakończ konfigurację

.parseSignedClaims(token)
└─ Rozpakuj token i sprawdź podpis

.getPayload()
└─ Pobierz payload (wszystkie dane)

.getSubject()
└─ Pobierz pole "sub" (username)
└─ Zwróć: "jan_kowalski"
```

**Rezultat:**
```
Wejście: token = "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJqYW5fa293YWxza2kiLCJy..."
Wyjście: username = "jan_kowalski"
```

---

## Metoda: validateJwtToken()

### Gdzie się wywołuje?
```
GET /api/demo/verify-token (z tokenem)
  ↓
AuthTokenFilter.doFilterInternal()
  ↓
if (jwtUtils.validateJwtToken(jwt))  ← TUTAJ
  ↓
true = Token OK ✅
false = Token błędny ❌
```

### Krok po Kroku

```java
public boolean validateJwtToken(String authToken) {
    try {
        Jwts.parser()
            .verifyWith((javax.crypto.SecretKey) getSigningKey())
            .build()
            .parseSignedClaims(authToken);
        return true;  // ✅ TOKEN JEST OK
    } catch (...) {
        logger.error(...);
        return false;  // ❌ TOKEN JEST USZKODZONY
    }
}
```

**Co się Sprawdza:**

| Test | Nazwa | Co Sprawdza | Jeśli Fail |
|------|-------|----------|-----------|
| 1 | **Podpis** | Czy token nie został zmieniony? | `MalformedJwtException` |
| 2 | **Wygaśnięcie** | Czy token nie wygasł? | `ExpiredJwtException` |
| 3 | **Algorytm** | Czy token używa HS256? | `UnsupportedJwtException` |
| 4 | **Format** | Czy token ma dane? | `IllegalArgumentException` |

---

### Błędy - Kiedy Się Zdarzają

#### 1. MalformedJwtException - "Token Jest Uszkodzony"

```
Przyczyny:
- Ktoś zmienił token w drodze
- Token ma tylko 2 części zamiast 3
- Token nie jest poprawnie sformatowany

Przykład:
Token: "eyJhbGciOiJIUzM4NCJ9"  ← Brakuje dwóch części!
Błąd: "JWT signature does not match"
```

#### 2. ExpiredJwtException - "Token Wygasł"

```
Przyczyny:
- Token jest stary
- Pole "exp" < dzisiejsza data

Przykład:
Token ważny do: 7 maja 20:00
Teraz jest: 8 maja 10:00
Błąd: "JWT expired at [...]"
```

#### 3. UnsupportedJwtException - "Zły Algorytm"

```
Przyczyny:
- Token był podpisany innym algorytmem
- My obsługujemy tylko HS256

Przykład:
Token podpisany RS256 zamiast HS256
Błąd: "JWT signature algorithm 'RS256' is not supported"
```

#### 4. IllegalArgumentException - "Pusty Token"

```
Przyczyny:
- Ktoś przesłał token pusty ""
- Authorization: Bearer (bez tokena)

Przykład:
Authorization: Bearer 
Błąd: "JWT claims string is empty"
```

---

## Flowchart - Cały Proces

### 🔄 Przepływ Rejestracji → Login → Weryfikacja

```
┌─────────────────────────────────────────────────────────────────┐
│ 1️⃣ REJESTRACJA (POST /api/auth/register)                        │
├─────────────────────────────────────────────────────────────────┤
│ Klient wysyła: {username: "jan", password: "pass"}              │
│ ↓                                                               │
│ Server: Sprawdź czy username istnieje                           │
│ ↓                                                               │
│ Server: Zapisz użytkownika z rolą KLIENT                        │
│ ↓                                                               │
│ Odpowiedź: 201 Created "User registered successfully!"          │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 2️⃣ LOGIN (POST /api/auth/login)                                 │
├─────────────────────────────────────────────────────────────────┤
│ Klient wysyła: {username: "jan", password: "pass"}              │
│ ↓                                                               │
│ Server: AuthRestController.authenticateUser()                   │
│ ↓                                                               │
│   1️⃣ authenticationManager.authenticate(username, password)     │
│       └─ Sprawdź hasło                                          │
│       └─ Jeśli błędne → 401 Unauthorized                        │
│       └─ Jeśli OK → Authentication object                       │
│   ↓                                                              │
│   2️⃣ jwtUtils.generateJwtToken(authentication)                  │
│       └─ Wyodrębnij username: "jan"                             │
│       └─ Wyodrębnij role: ["ROLE_KLIENT"]                       │
│       └─ Buď token: HEADER.PAYLOAD.SIGNATURE                    │
│       └─ Podpisz kluczem jwtSecret                              │
│       └─ Zwróć token                                            │
│   ↓                                                              │
│   3️⃣ Zwróć odpowiedź                                            │
│       {                                                         │
│         "token": "eyJhbGciOiJIUzM4NCJ9...",                     │
│         "username": "jan",                                      │
│         "role": "KLIENT"                                        │
│       }                                                         │
│ ↓                                                               │
│ Odpowiedź: 200 OK + TOKEN                                       │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 3️⃣ UŻYCIE TOKENA (GET /api/demo/verify-token)                   │
├─────────────────────────────────────────────────────────────────┤
│ Klient wysyła:                                                  │
│   Authorization: Bearer eyJhbGciOiJIUzM4NCJ9...                 │
│ ↓                                                               │
│ Server: AuthTokenFilter.doFilterInternal()                      │
│ ↓                                                               │
│   1️⃣ Wyodrębnij token z nagłówka                                │
│       token = "eyJhbGciOiJIUzM4NCJ9..." (bez "Bearer ")         │
│   ↓                                                              │
│   2️⃣ jwtUtils.validateJwtToken(token)                           │
│       └─ Sprawdź podpis (czy token nie był zmieniony)           │
│       └─ Sprawdź wygaśnięcie (czy token nie wygasł)             │
│       └─ Zwróć: true ✅ lub false ❌                            │
│   ↓                                                              │
│       Jeśli false:                                              │
│       └─ Wysłij 401 Unauthorized - "Token invalid"              │
│       └─ Przerwij przetwarzanie                                 │
│   ↓                                                              │
│       Jeśli true:                                               │
│       └─ jwtUtils.getUserNameFromJwtToken(token)                │
│       └─ Zwróć: "jan"                                           │
│   ↓                                                              │
│   3️⃣ userDetailsService.loadUserByUsername("jan")              │
│       └─ Pobierz dane użytkownika z bazy                        │
│       └─ Zwróć UserDetails z rolami                             │
│   ↓                                                              │
│   4️⃣ SecurityContextHolder.getContext().setAuthentication()    │
│       └─ Ustaw kontekst bezpieczeństwa                          │
│       └─ Role są teraz dostępne dla całego żądania              │
│   ↓                                                              │
│ Server: TokenDemoController.verifyToken()                       │
│ ↓                                                               │
│ Odpowiedź: 200 OK                                               │
│ {                                                               │
│   "message": "Token jest poprawny",                             │
│   "username": "jan",                                            │
│   "roles": ["ROLE_KLIENT"],                                     │
│   "expiration": "2026-05-08T20:00:00Z"                          │
│ }                                                               │
└─────────────────────────────────────────────────────────────────┘
```

---

## 📊 Podsumowanie - Co Musisz Zapamiętać

### JwtUtils ma 3 Główne Metody

| Metoda | Wejście | Wyjście | Kiedy |
|--------|---------|---------|-------|
| `generateJwtToken()` | `Authentication` (zalogowany user) | `String` (token JWT) | Po zalogowaniu |
| `getUserNameFromJwtToken()` | `String` (token JWT) | `String` (username) | Na każde żądanie |
| `validateJwtToken()` | `String` (token JWT) | `boolean` (OK czy nie) | Na każde żądanie |

### JWT Ma 3 Części

```
HEADER.PAYLOAD.SIGNATURE

HEADER = {"alg": "HS256"}
PAYLOAD = {"sub": "jan", "roles": ["ROLE_KLIENT"], "iat": ..., "exp": ...}
SIGNATURE = HASH(header + payload + secret_key)
```

### Token Jest Bezpieczny Bo

1. **Podpisany** - nikt nie może go zmienić bez tajnego klucza
2. **Zawiera role** - serwer wie jakie dostępy ma użytkownik
3. **Ma wygaśnięcie** - jeśli zostanie skradziony, będzie działać tylko ograniczony czas
4. **Można sprawdzić** - serwer zawsze może zweryfikować czy jest poprawny

---

**Data:** 7 maja 2026  
**Status:** ✅ Pełne Wyjaśnienie Kodu
