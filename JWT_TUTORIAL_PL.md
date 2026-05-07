# 🔐 Mechanizm JWT w Projekcie Barber - Poradnik Nauki

## 📋 Spis Treści
1. [Jak działa JWT](#jak-działa-jwt)
2. [Przepływ Autentykacji](#przepływ-autentykacji)
3. [Klucze Klasy i Kod](#klucze-klasy-i-kod)
4. [Jak Testować](#jak-testować)

---

## Jak Działa JWT

**JWT (JSON Web Token)** to bezpieczny sposób przesyłania danych między serwerem a klientem.

```
Token JWT ma 3 części:
┌─────────────┬──────────────┬───────────────┐
│   HEADER    │   PAYLOAD    │   SIGNATURE   │
├─────────────┼──────────────┼───────────────┤
│ Algorytm    │ Dane         │ Weryfikacja   │
│ (HS256)     │ (username,   │ (Secret Key)  │
│             │  role, exp)  │               │
└─────────────┴──────────────┴───────────────┘
```

---

## Przepływ Autentykacji (Krok po Kroku)

```
KLIENT                           SERWER
  │                                 │
  │ 1️⃣ POST /api/auth/register     │
  ├────────────────────────────────>│
  │ (username, password, email)     │
  │                                 │ Zapisz użytkownika
  │                                 │ Domyślna rola: KLIENT
  │ 201 Created                     │
  │<────────────────────────────────┤
  │                                 │
  │ 2️⃣ POST /api/auth/login        │
  ├────────────────────────────────>│
  │ (username, password)            │
  │                                 │ Sprawdź dane
  │ 200 OK + JWT TOKEN + ROLE       │ Wygeneruj token
  │<────────────────────────────────┤
  │ {                               │
  │   "token": "eyJhbG...",        │
  │   "username": "demoUser",       │
  │   "role": "KLIENT"              │
  │ }                               │
  │                                 │
  │ 3️⃣ GET /api/demo/verify-token │
  ├────────────────────────────────>│
  │ Authorization: Bearer TOKEN     │
  │                                 │ Waliduj token
  │ 200 OK + Szczegóły Tokena       │ Odczytaj rolę
  │<────────────────────────────────┤
```

---

## Klucze Klasy i Kod

### 1️⃣ **JwtUtils.java** - Generowanie i Walidacja Tokenów

**Lokalizacja:** `src/main/java/com/example/barber/security/JwtUtils.java`

#### Generowanie Tokena (z rolą!)
```java
public String generateJwtToken(Authentication authentication) {
    UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();

    // 🔑 KLUCZOWE: Wyodrębnij role z użytkownika
    java.util.List<String> roles = userPrincipal.getAuthorities().stream()
            .map(org.springframework.security.core.GrantedAuthority::getAuthority)
            .collect(java.util.stream.Collectors.toList());

    return Jwts.builder()
            .subject((userPrincipal.getUsername()))        // Kto to? (username)
            .claim("roles", roles)                         // ⭐ ROLA W TOKENIE
            .issuedAt(new Date())                          // Kiedy wydany?
            .expiration(new Date(...))                     // Kiedy wygaśnie?
            .signWith(getSigningKey())                     // 🔐 Podpisz tajnym kluczem
            .compact();
}
```

**Co się dzieje:**
- `subject()` → wpisuje username do tokena
- `claim("roles", roles)` → wpisuje role do tokena ⭐
- `signWith()` → podpisuje token, aby nikt nie mógł go sfałszować

#### Walidacja Tokena
```java
public boolean validateJwtToken(String authToken) {
    try {
        Jwts.parser()
            .verifyWith((javax.crypto.SecretKey) getSigningKey())
            .build()
            .parseSignedClaims(authToken);                 // Sprawdź podpis
        return true;
    } catch (ExpiredJwtException e) {
        logger.error("JWT token is expired");              // Token stary
        return false;
    } catch (MalformedJwtException e) {
        logger.error("Invalid JWT token");                 // Token uszkodzony
        return false;
    }
}
```

#### Odczyt Danych z Tokena
```java
public String getUserNameFromJwtToken(String token) {
    return Jwts.parser()
            .verifyWith((javax.crypto.SecretKey) getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getSubject();                                 // Czytaj username
}
```

---

### 2️⃣ **AuthRestController.java** - Rejestracja i Login

**Lokalizacja:** `src/main/java/com/example/barber/controller/api/AuthRestController.java`

#### Rejestracja (POST /api/auth/register)
```java
@PostMapping("/register")
public ResponseEntity<?> registerUser(@RequestBody User user) {
    if (userRepo.findByUsername(user.getUsername()) != null) {
        return ResponseEntity.badRequest().body("Username już istnieje!");
    }

    user.setRole("KLIENT");  // ⭐ Domyślna rola
    userRepo.save(user);

    return ResponseEntity.ok("User registered successfully!");
}
```

**Ważne:** Każdy nowy użytkownik dostaje rolę `KLIENT`

#### Login (POST /api/auth/login)
```java
@PostMapping("/login")
public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
    // 1. Sprawdź username i password
    Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                loginRequest.getUsername(), 
                loginRequest.getPassword()
            )
    );

    // 2. Ustaw kontekst bezpieczeństwa
    SecurityContextHolder.getContext().setAuthentication(authentication);

    // 3. Wygeneruj JWT (z rolą!)
    String jwt = jwtUtils.generateJwtToken(authentication);

    // 4. Pobierz użytkownika z bazy
    User user = userRepo.findByUsername(loginRequest.getUsername());

    // 5. Zwróć token + rola
    return ResponseEntity.ok(new JwtResponse(jwt, user.getUsername(), user.getRole()));
}
```

**Odpowiedź:**
```json
{
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJkZW1vVXNlciIsInJvbGVzIjpbIlJPTEVfS0xJRU5UIl0sImlhdCI6MTcxNTExMjEyMCwiZXhwIjoxNzE1MTk4NTIwfQ...",
    "username": "demoUser",
    "role": "KLIENT"
}
```

---

### 3️⃣ **TokenDemoController.java** - Weryfikacja Tokena

**Lokalizacja:** `src/main/java/com/example/barber/controller/api/TokenDemoController.java`

```java
@GetMapping("/verify-token")
public ResponseEntity<?> verifyToken(@RequestHeader("Authorization") String authHeader) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        return ResponseEntity.badRequest().body("Brak Authorization header!");
    }

    // Wyodrębnij token (usuń "Bearer ")
    String token = authHeader.substring(7);

    try {
        // 🔍 Rozpakuj i zweryfikuj token
        Claims claims = Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        // 📋 Przygotuj odpowiedź
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Token jest poprawny");
        response.put("username", claims.getSubject());       // Odczytaj username
        response.put("roles", claims.get("roles"));          // ⭐ Odczytaj role
        response.put("expiration", claims.getExpiration());  // Kiedy wygaśnie?

        return ResponseEntity.ok(response);
    } catch (Exception e) {
        return ResponseEntity.badRequest().body("Błąd weryfikacji: " + e.getMessage());
    }
}
```

**Odpowiedź:**
```json
{
    "message": "Token jest poprawny",
    "username": "demoUser",
    "roles": ["ROLE_KLIENT"],
    "expiration": "2025-05-07T21:35:20Z"
}
```

---

### 4️⃣ **AuthTokenFilter.java** - Filtr Automatycznej Walidacji

**Lokalizacja:** `src/main/java/com/example/barber/security/AuthTokenFilter.java`

```java
@Override
protected void doFilterInternal(HttpServletRequest request, 
                               HttpServletResponse response, 
                               FilterChain filterChain) throws ServletException, IOException {
    try {
        // 1. Wyodrębnij token z nagłówka "Authorization: Bearer TOKEN"
        String jwt = parseJwt(request);

        // 2. Jeśli jest token i jest poprawny
        if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
            // 3. Odczytaj username
            String username = jwtUtils.getUserNameFromJwtToken(jwt);

            // 4. Pobierz dane użytkownika z bazy
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // 5. Utwórz obiekt autentykacji
            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()  // ⭐ Role będą dostępne!
                );

            // 6. Ustaw w kontekście bezpieczeństwa
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
    } catch (Exception e) {
        logger.error("Cannot set user authentication: {}", e.getMessage());
    }

    // Kontynuuj dalej w łańcuchu filtrów
    filterChain.doFilter(request, response);
}

private String parseJwt(HttpServletRequest request) {
    String headerAuth = request.getHeader("Authorization");
    
    if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
        return headerAuth.substring(7);  // Usuń "Bearer "
    }
    return null;
}
```

**Jak to działa:**
1. Na każde żądanie HTTP filtr sprawdza nagłówek `Authorization`
2. Jeśli jest token `Bearer XYZ`, automatycznie go waliduje
3. Jeśli token jest poprawny, ustawia użytkownika w kontekście bezpieczeństwa
4. Role z tokena są dostępne dla @PreAuthorize itp.

---

### 5️⃣ **SecurityConfig.java** - Konfiguracja Bezpieczeństwa

**Lokalizacja:** `src/main/java/com/example/barber/config/SecurityConfig.java`

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            // ✅ Publiczne endpointy (brak tokena potrzebnego)
            .requestMatchers("/api/auth/**").permitAll()
            .requestMatchers("/login", "/register", "/css/**").permitAll()
            
            // ✅ Chronione endpointy (token wymagany + rola)
            .requestMatchers("/api/users/**").hasAnyRole("ADMIN", "MANAGER", "USER", "BARBER", "KLIENT")
            .requestMatchers("/api/tasks/**").hasAnyRole("ADMIN", "MANAGER", "BARBER")
            .requestMatchers("/api/reservations/**").hasAnyRole("ADMIN", "MANAGER", "BARBER", "KLIENT")
            .requestMatchers("/barber/**").hasRole("BARBER")
            .requestMatchers("/client/**").hasRole("KLIENT")
            .anyRequest().authenticated()
        );

    // ⭐ Dodaj nasz JWT filtr
    http.addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
}
```

**Znaczenie:**
- `permitAll()` → nie trzeba tokena
- `hasRole("KLIENT")` → token wymagany, rola musi być "KLIENT"
- `addFilterBefore()` → nasz filtr JWT sprawdza token PRZED standardowym filtrem

---

## Jak Testować

### 🧪 Scenariusz 1: Pełny Test (REST Client w VS Code)

**Wymagane rozszerzenie:** REST Client (Huachao Mao)

1. Zainstaluj rozszerzenie REST Client w VS Code
2. Otwórz plik [demo.http](demo.http)
3. Klikaj na "Send Request" nad każdym żądaniem

**Krok po kroku:**

```http
### 1️⃣ REJESTRACJA - Utwórz nowego użytkownika
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "username": "testUser123",
  "password": "testPass123",
  "email": "test@example.com",
  "firstName": "Jan",
  "lastName": "Kowalski"
}

# Oczekiwana odpowiedź: 200 OK - User registered successfully!
```

```http
### 2️⃣ LOGIN - Zaloguj się i otrzymaj token
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "testUser123",
  "password": "testPass123"
}

# Oczekiwana odpowiedź:
# {
#   "token": "eyJhbGciOiJIUzI1NiJ9...",
#   "username": "testUser123",
#   "role": "KLIENT"
# }

# ⭐ Token jest automatycznie zapisywany w {{auth_token}}
```

```http
### 3️⃣ WERYFIKACJA - Sprawdź token i odczytaj rolę
GET http://localhost:8080/api/demo/verify-token
Authorization: Bearer {{auth_token}}

# Oczekiwana odpowiedź:
# {
#   "message": "Token jest poprawny",
#   "username": "testUser123",
#   "roles": ["ROLE_KLIENT"],
#   "expiration": "2026-05-08T20:35:20Z"
# }
```

---

### 🔍 Jak Dekodować Token (online)

Otwórz https://jwt.io/ i wklej token z odpowiedzi logowania.

**Zobaczysz:**
```json
HEADER:
{
  "alg": "HS256"
}

PAYLOAD:
{
  "sub": "testUser123",
  "roles": ["ROLE_KLIENT"],
  "iat": 1715115420,
  "exp": 1715201820
}

SIGNATURE:
hmacShaKeyFor("myVerySecretKeyForBarberAppThatIsAtLeast32CharactersLong")
```

---

### 📝 Ważne Klasy do Zapamiętania

| Klasa | Co Robi | Plik |
|-------|---------|------|
| `JwtUtils` | Generuje i waliduje tokeny | `security/JwtUtils.java` |
| `AuthRestController` | Endpoints do rejestracji i logowania | `controller/api/AuthRestController.java` |
| `TokenDemoController` | Endpoint do weryfikacji tokena | `controller/api/TokenDemoController.java` |
| `AuthTokenFilter` | Filtr sprawdzający token na każde żądanie | `security/AuthTokenFilter.java` |
| `SecurityConfig` | Reguły dostępu (jakie role dla jakich endpointów) | `config/SecurityConfig.java` |

---

### 🎓 Co Musisz Zapamiętać na Jutro

1. **JWT** = Token z 3 częściami (header, payload, signature)
2. **Payload zawiera:**
   - `sub` (subject) = username
   - `roles` = lista ról użytkownika
   - `iat` = kiedy wydany
   - `exp` = kiedy wygaśnie

3. **Przepływ:**
   - Rejestracja → rola jest ustawiana (`KLIENT`)
   - Login → JWT jest generowany z rolą
   - Token jest wysyłany w `Authorization: Bearer TOKEN`
   - Filtr automatycznie sprawdza i ustawia kontekst

4. **Bezpieczeństwo:**
   - Token jest podpisany tajnym kluczem
   - Nikt nie może go sfałszować bez klucza
   - Każdy żądanie z tokenem przechodzi przez `AuthTokenFilter`

---

### 💡 Pytania do Samochodzenia

- Co się stanie jeśli pošłę żądanie bez tokena?
- Co się stanie jeśli zmienię datę wygaśnięcia w tokenie?
- Gdzie przechowywane są role użytkownika?
- Co to jest `@PreAuthorize` i jak się go używa?
- Czy mogę mieć użytkownika z wieloma rolami?

---

**Data:** 7 maja 2026  
**Status:** ✅ Serwer uruchomiony na `http://localhost:8080`  
**Baza danych:** H2 (w pamięci) dostępna na `http://localhost:8080/h2-console`
