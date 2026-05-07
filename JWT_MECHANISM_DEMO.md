# 🔐 Mechanizm JWT - Generowanie i Weryfikacja Tokenów

## 📋 Zawartość

Ten dokument opisuje **kompletny mechanizm generowania i weryfikacji tokenów JWT** z informacją o roli użytkownika w aplikacji Barber.

---

## 🏗️ Architektura Systemu

```
┌─────────────┐
│   Klient    │
│  (Browser)  │
└──────┬──────┘
       │ 1. POST /api/auth/login
       │ (username, password)
       ▼
┌──────────────────────────────────┐
│   AuthRestController             │
│  - Uwierzytelnia użytkownika     │
│  - Generuje JWT token            │
└─────────────┬────────────────────┘
              │ 2. Zwraca token
              │    (zawiera roję)
              ▼
         JWT TOKEN
      (w odpowiedzi)
              │
       Klient zapisuje
         token
              │
       3. Wysyła w nagłówku
          Authorization: Bearer <TOKEN>
              │
              ▼
┌──────────────────────────────────┐
│   AuthTokenFilter                │
│  - Wyciąga token z nagłówka      │
│  - Waliduje token                │
│  - Wyciąga username i role       │
│  - Ustawia SecurityContext       │
└─────────────┬────────────────────┘
              │
              ▼
┌──────────────────────────────────┐
│   SecurityConfig                 │
│  - Sprawdza uprawnienia           │
│  - Pozwala/Blokuje dostęp        │
└──────────────────────────────────┘
```

---

## 🔑 Co zawiera JWT Token?

Nasz token zawiera następujące informacje (claims):

```json
{
  "sub": "username",              // Username (Subject)
  "roles": ["ROLE_BARBER"],       // Role użytkownika
  "iat": 1234567890,              // Issued At (czas utworzenia)
  "exp": 1234654290,              // Expiration (czas wygaśnięcia)
  "signature": "..."              // Podpis (HMAC-SHA256)
}
```

### 📝 Ejemplo Tokenu JWT

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.
eyJzdWIiOiJiYXJiZXIxIiwicm9sZXMiOlsiQkFSQkVSIl0sImlhdCI6MTcxNTA0MTIwMCwiZXhwIjoxNzE1MTI3NjAwfQ.
abc123xyz789...
```

Token ma 3 części separowane punktami:
1. **Header** - typ tokenu i algorytm (HMAC-SHA256)
2. **Payload** - dane (username, role, czasów)
3. **Signature** - podpis dla weryfikacji

---

## 📁 Pliki Kluczowe

### 1. `JwtUtils.java` - Generowanie i Weryfikacja

```java
// Generowanie tokenu z rolami
public String generateJwtToken(Authentication authentication) {
    UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
    List<String> roles = userPrincipal.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.toList());
    
    return Jwts.builder()
        .subject(userPrincipal.getUsername())
        .claim("roles", roles)          // ← Role w token!
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
        .signWith(getSigningKey())
        .compact();
}

// Walidacja tokenu
public boolean validateJwtToken(String authToken) {
    try {
        Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(authToken);
        return true;
    } catch (ExpiredJwtException e) {
        logger.error("JWT token is expired");
    } catch (MalformedJwtException e) {
        logger.error("Invalid JWT token");
    }
    return false;
}

// Wyciągnięcie roli z tokenu
public List<String> getRolesFromJwtToken(String token) {
    var claims = Jwts.parser().verifyWith(getSigningKey())
        .build().parseSignedClaims(token).getPayload();
    Object rolesObj = claims.get("roles");
    if (rolesObj instanceof List) {
        return (List<String>) rolesObj;
    }
    return Collections.emptyList();
}
```

### 2. `AuthTokenFilter.java` - Filtr na każde żądanie

```java
@Component
public class AuthTokenFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) {
        try {
            // 1. Pobierz token z nagłówka
            String jwt = parseJwt(request);
            
            // 2. Waliduj token
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                // 3. Wyciągnij username
                String username = jwtUtils.getUserNameFromJwtToken(jwt);
                
                // 4. Załaduj dane użytkownika (z rolami)
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                
                // 5. Utwórz Authentication
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(userDetails, null, 
                                                           userDetails.getAuthorities());
                
                // 6. Ustaw w SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e.getMessage());
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);  // Usuń "Bearer " prefix
        }
        return null;
    }
}
```

### 3. `AuthRestController.java` - Logowanie i Rejestracja

```java
@PostMapping("/login")
public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
    // 1. Uwierzytelni użytkownika
    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            loginRequest.getUsername(), 
            loginRequest.getPassword()
        )
    );
    
    // 2. Ustaw w SecurityContext
    SecurityContextHolder.getContext().setAuthentication(authentication);
    
    // 3. Generuj JWT token (z rolą!)
    String jwt = jwtUtils.generateJwtToken(authentication);
    
    // 4. Zwróć token i info o użytkowniku
    User user = userRepo.findByUsername(loginRequest.getUsername());
    return ResponseEntity.ok(new JwtResponse(jwt, user.getUsername(), user.getRole()));
}
```

### 4. `SecurityConfig.java` - Konfiguracja Bezpieczeństwa

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/auth/**").permitAll()  // Logowanie dostępne dla wszystkich
            .requestMatchers("/api/demo/**").permitAll()  // Demo dostępne dla wszystkich
            .requestMatchers("/barber/**").hasRole("BARBER")  // Tylko BARBER
            .requestMatchers("/client/**").hasRole("KLIENT")  // Tylko KLIENT
            .requestMatchers("/api/tasks/**").hasAnyRole("BARBER", "KLIENT")
            .anyRequest().authenticated()
        )
        .addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);  // ← Dodaj nasz filtr!
    
    return http.build();
}
```

### 5. `TokenDemoController.java` - Endpointy Demonstracyjne

```java
@GetMapping("/verify-token")
public ResponseEntity<?> verifyToken(@RequestHeader("Authorization") String authHeader) {
    // Weryfikuje token i zwraca dane
}

@PostMapping("/decode-token")
public ResponseEntity<?> decodeToken(@RequestBody Map<String, String> request) {
    // Dekoduje token (bez weryfikacji)
}

@GetMapping("/user-info")
public ResponseEntity<?> getUserInfo() {
    // Zwraca dane zalogowanego użytkownika
}

@GetMapping("/has-role/{role}")
public ResponseEntity<?> hasRole(@PathVariable String role) {
    // Sprawdza czy użytkownik ma daną rolę
}

@GetMapping("/my-roles")
public ResponseEntity<?> getMyRoles() {
    // Zwraca listę wszystkich ról użytkownika
}
```

---

## 🚀 Jak Testować?

### 1. Uruchom Aplikację

```bash
cd c:\Users\kondz1o\Desktop\Barber\Barber
mvnw spring-boot:run
```

Aplikacja uruchomi się na `http://localhost:8080`

### 2. Otwórz plik `demo.http`

W VS Code otwórz plik `demo.http` w projekcie.

### 3. Wykonuj Żądania po Kolei

Kliknij **"Send Request"** dla każdego żądania w kolejności:

#### **Faza 1: Rejestracja Użytkowników** (Kroki 1️⃣-3️⃣)

```http
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "username": "klient1",
  "password": "haslo123",
  "email": "klient@example.com",
  "firstName": "Jan",
  "lastName": "Kowalski",
  "role": "KLIENT"
}
```

✅ Odpowiedź: `"User registered successfully!"`

#### **Faza 2: Generowanie Tokenów** (Kroki 4️⃣-6️⃣)

```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "klient1",
  "password": "haslo123"
}
```

✅ Odpowiedź:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "klient1",
  "role": "KLIENT"
}
```

> Token zostanie **automatycznie zapisany** w zmiennej `{{klient_token}}`

#### **Faza 3: Weryfikacja Tokenów** (Kroki 7️⃣-9️⃣)

```http
GET http://localhost:8080/api/demo/verify-token
Authorization: Bearer {{klient_token}}
```

✅ Odpowiedź:
```json
{
  "message": "✅ Token jest poprawny",
  "username": "klient1",
  "roles": ["KLIENT"],
  "issuedAt": "2026-05-07T10:00:00Z",
  "expiration": "2026-05-08T10:00:00Z",
  "authenticated_user": "klient1",
  "authorities": ["ROLE_KLIENT"]
}
```

#### **Faza 4: Sprawdzenie Roli** (Kroki 1️⃣5️⃣-2️⃣2️⃣)

```http
GET http://localhost:8080/api/demo/has-role/KLIENT
Authorization: Bearer {{klient_token}}
```

✅ Odpowiedź:
```json
{
  "role": "KLIENT",
  "hasRole": true,
  "username": "klient1",
  "message": "✅ Masz rolę: KLIENT"
}
```

#### **Faza 5: Testy Błędów** (Kroki 2️⃣3️⃣-2️⃣5️⃣)

```http
GET http://localhost:8080/api/demo/verify-token
Authorization: Bearer INVALID_TOKEN_HERE
```

❌ Odpowiedź:
```json
{
  "message": "❌ Błąd weryfikacji tokenu",
  "error": "JWT signature does not match"
}
```

#### **Faza 6: Kontrola Dostępu na Podstawie Ról** (Kroki 2️⃣6️⃣-3️⃣1️⃣)

```http
GET http://localhost:8080/barber/dashboard
Authorization: Bearer {{klient_token}}
```

❌ Odpowiedź: `403 Forbidden` (Klient nie ma roli BARBER)

```http
GET http://localhost:8080/barber/dashboard
Authorization: Bearer {{barber_token}}
```

✅ Odpowiedź: `200 OK` (Barber ma uprawnienia)

---

## 🔄 Flow Bezpieczeństwa

```
Użytkownik
    │
    ├─ Logowanie: POST /api/auth/login
    │  └─> Generowanie JWT: jwtUtils.generateJwtToken()
    │      └─> Token z rolą: {"username": "...", "roles": [...]}
    │
    └─> Token przechowywany w LocalStorage
        │
        ├─ Każde żądanie API
        │  └─> Header: Authorization: Bearer <TOKEN>
        │
        ├─ Filtr: AuthTokenFilter.doFilterInternal()
        │  ├─> Wyciągnij token z nagłówka
        │  ├─> Waliduj: validateJwtToken()
        │  ├─> Wyciągnij username: getUserNameFromJwtToken()
        │  ├─> Załaduj role: loadUserByUsername()
        │  └─> Ustaw Authentication w SecurityContext
        │
        ├─ Security Config
        │  ├─> Sprawdź role w SecurityContext
        │  ├─> /barber/** → hasRole("BARBER")
        │  ├─> /client/** → hasRole("KLIENT")
        │  └─> Pozwól/Blokuj dostęp
        │
        └─> Zwróć zasób lub 403 Forbidden
```

---

## 📊 Role w Systemie

| Rola      | Uprawnienia                          | Dostęp                |
|-----------|--------------------------------------|-----------------------|
| **KLIENT**| Rezerwowanie usług                   | `/client/**`          |
| **BARBER**| Zarządzanie zadaniami, rezerwacjami  | `/barber/**`          |
| **ADMIN** | Dostęp do wszystkich zasobów         | `/admin/**`           |

---

## ⚙️ Konfiguracja

### `application.properties`

```properties
# JWT Configuration
barber.app.jwtSecret=myVerySecretKeyForBarberAppThatIsAtLeast32CharactersLong
barber.app.jwtExpirationMs=86400000  # 1 dzień (w milisekundach)
```

### Zmiana Czasu Wygaśnięcia Tokenu

Aby zmienić czas ważności tokenu, edytuj:

```properties
# 1 godzina
barber.app.jwtExpirationMs=3600000

# 7 dni
barber.app.jwtExpirationMs=604800000

# 30 dni
barber.app.jwtExpirationMs=2592000000
```

---

## 🔒 Bezpieczeństwo

### ✅ Dobre Praktyki (Już Wdrożone)

1. **Token ze Stronę Serwera** - Tajny klucz nigdy nie opuszcza serwera
2. **Podpis HMAC-SHA256** - Niemożliwe psucie tokenu bez klucza
3. **Expiration** - Token wygasa po 1 dniu
4. **Rola w Token** - Brak konieczności sprawdzania bazy danych na każde żądanie
5. **Filtr na Każde Żądanie** - Każde żądanie jest weryfikowane

### ⚠️ Rekomendacje dla Produkcji

1. **Użyj zmiennych środowiskowych na kluczy JWT**
   ```bash
   set BARBER_APP_JWT_SECRET=super_secret_key_min_32_chars
   ```

2. **HTTPS obowiązkowe** - Token zawsze w zabezpieczonym kanale

3. **Refresh Tokens** - Dodaj token refresh dla długotrwałych sesji

4. **CORS Ograniczenia** - Odogranicz dostęp do konkretnych domen
   ```java
   @CrossOrigin(origins = "https://yourdomain.com", maxAge = 3600)
   ```

5. **Rate Limiting** - Ogranicz ilość żądań logowania

---

## 📚 Dodatkowe Endpointy do Testowania

W `demo.http` znajdziesz:

- ✅ **Rejestracja użytkowników** (z rolami)
- ✅ **Logowanie** (generowanie tokenów)
- ✅ **Weryfikacja tokenów** (sprawdzenie ważności)
- ✅ **Dekodowanie tokenów** (odczyt danych)
- ✅ **Informacje użytkownika** (dane zalogowanego użytkownika)
- ✅ **Sprawdzenie roli** (czy użytkownik ma daną rolę)
- ✅ **Lista ról** (wszystkie role użytkownika)
- ✅ **Testy błędów** (brak tokenu, niepoprawny token)
- ✅ **Kontrola dostępu** (role-based access control)

---

## 🎯 Podsumowanie

### Co Zostało Wdrożone?

1. ✅ **Generowanie JWT Tokenów** - z rolami użytkownika
2. ✅ **Weryfikacja Tokenów** - na każde żądanie HTTP
3. ✅ **Wyciągnięcie Roli** - z tokenu JWT
4. ✅ **Kontrola Dostępu** - na podstawie ról (BARBER, KLIENT, ADMIN)
5. ✅ **Demo Endpointy** - do testowania mechanizmu
6. ✅ **Kompletny Test Suite** - w pliku `demo.http`

### Co Musisz Zrobić Teraz?

1. Uruchom aplikację: `mvnw spring-boot:run`
2. Otwórz plik `demo.http` w VS Code
3. Wykonuj żądania po kolei, zaczynając od kroku 1️⃣
4. Obserwuj jak tokeny są generowane, weryfikowane i jak działają role

Zabawy! 🚀
