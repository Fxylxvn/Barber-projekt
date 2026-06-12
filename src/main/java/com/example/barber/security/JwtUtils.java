package com.example.barber.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

/*
  Klasa narzędziowa odpowiedzialna za wszystkie operacje na tokenach JWT
  (JSON Web Token) w aplikacji Barber.

  Jej główne zadania to:

    Generowanie tokenu JWT po pomyślnym uwierzytelnieniu użytkownika.
    Walidacja tokenu (sprawdzenie podpisu, czasu ważności i poprawności formatu).
    Odczyt danych (username, role, czas ważności) zapisanych wewnątrz tokenu.</li>
  </ul>
  Token jest podpisywany algorytmem HMAC-SHA256 z kluczem wczytywanym z pliku
  konfiguracyjnego ({@code application.properties}).


  <p>Wstrzykiwana jako bean Spring ({@code @Component}) do {@link AuthTokenFilter}
  i kontrolerów REST.</p>
 */
@Component
public class JwtUtils {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    /*
      Tajny klucz do podpisywania tokenów JWT.
      Wczytywany z właściwości {@code barber.app.jwtSecret}.
      Musi mieć co najmniej 32 znaki (wymaganie algorytmu HS256).
     */
    @Value("${barber.app.jwtSecret:myVerySecretKeyForBarberAppThatIsAtLeast32CharactersLong}")
    private String jwtSecret;

    /*
      Czas ważności tokenu JWT w milisekundach.
      Domyślna wartość: {@code 86400000} ms = 24 godziny.
      Wczytywany z właściwości {@code barber.app.jwtExpirationMs}.
     */
    @Value("${barber.app.jwtExpirationMs:86400000}")
    private int jwtExpirationMs;

    /*
      Generuje token JWT dla uwierzytelnionego użytkownika.

      Token zawiera:
        subject – nazwa użytkownika (login).
        claim "roles" – lista ról użytkownika, np. {@code ["ROLE_BARBER"]}.
        issuedAt – czas wystawienia tokenu.
        expiration – czas wygaśnięcia (teraz + {@code jwtExpirationMs}).
        podpis HMAC-SHA256 – zabezpieczenie przed fałszowaniem.
     */
    public String generateJwtToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();

        // Wyodrębnij wszystkie role użytkownika i zbierz je w listę
        java.util.List<String> roles = userPrincipal.getAuthorities().stream()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                .collect(java.util.stream.Collectors.toList());

        return Jwts.builder()
                .subject((userPrincipal.getUsername()))   // kto jest właścicielem tokenu
                .claim("roles", roles)                    // dodaj role do ładunku tokenu
                .issuedAt(new Date())                     // czas wystawienia
                .expiration(new Date((new Date()).getTime() + jwtExpirationMs)) // czas wygaśnięcia
                .signWith(getSigningKey())                // podpisz kluczem HMAC-SHA256
                .compact();                               // skompaktuj do stringa JWT
    }

    /*
      Tworzy kryptograficzny klucz HMAC-SHA256 na podstawie tajnego ciągu znaków.

      <p>Metoda pomocnicza używana wewnętrznie do podpisywania i weryfikacji tokenów.</p>

      @return klucz {@link Key} gotowy do użycia w algorytmie HS256
     */
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /*
      Wyodrębnia nazwę użytkownika (subject) z tokenu JWT.

      <p>Używane przez {@link AuthTokenFilter} po udanej walidacji tokenu,
      aby wiedzieć, którego użytkownika załadować z bazy danych.</p>

      @param token token JWT jako string
      @return nazwa użytkownika zapisana w polu {@code sub} tokenu
     */
    public String getUserNameFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /*
      Wyodrębnia listę ról zapisanych w tokenie JWT.

      <p>Role są przechowywane w claim'ie {@code "roles"} jako lista stringów.</p>

      @param token token JWT jako string
      @return lista ról użytkownika lub pusta lista, jeśli brak pola "roles"
     */
    public java.util.List<String> getRolesFromJwtToken(String token) {
        var claims = Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof java.util.List) {
            return (java.util.List<String>) rolesObj;
        }
        return java.util.Collections.emptyList();
    }

    /*
      Zwraca wszystkie dane (claims) zawarte w tokenie JWT jako mapę.

      <p>Używane głównie w celach diagnostycznych i demonstracyjnych
      (np. endpoint {@code /api/demo/verify-token}).</p>

      @param token token JWT jako string
      @return mapa z kluczami: {@code username}, {@code roles}, {@code issuedAt}, {@code expiration}
     */
    public java.util.Map<String, Object> getClaimsFromJwtToken(String token) {
        var claims = Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("username", claims.getSubject());
        result.put("roles", claims.get("roles"));
        result.put("issuedAt", claims.getIssuedAt());
        result.put("expiration", claims.getExpiration());
        return result;
    }

    /*
      Sprawdza, czy podany token JWT jest ważny.

      <p>Weryfikacja obejmuje:
      <ol>
        <li>Poprawność podpisu kryptograficznego (wykrywa falsyfikaty).</li>
        <li>Czas ważności – token nie może być wygasły.</li>
        <li>Poprawność formatu JWT (nagłówek, ładunek, podpis).</li>
        <li>Obecność obowiązkowych pól.</li>
      </ol>
      Każdy z tych warunków, jeśli niespełniony, rzuca wyjątek i metoda zwraca {@code false}.</p>

      @param authToken token JWT do zwalidowania
      @return {@code true} jeśli token jest ważny, {@code false} w przeciwnym razie
     */
    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser().verifyWith((javax.crypto.SecretKey) getSigningKey()).build().parseSignedClaims(authToken);
            return true;
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
}
