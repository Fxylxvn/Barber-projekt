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

@Component
public class JwtUtils {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);
/* Celem tej klasy jest generowanie i odczytywanie tokenów JWT w aplikacji */
    @Value("${barber.app.jwtSecret:myVerySecretKeyForBarberAppThatIsAtLeast32CharactersLong}")
    private String jwtSecret; // tajny klucz do podpisywania tokenów

    @Value("${barber.app.jwtExpirationMs:86400000}")
    private int jwtExpirationMs;
 // czas wygasniecia tokena ustawiony na 1 dzien
    public String generateJwtToken(Authentication authentication) {
        // generowanie tokena JWT
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal(); // Wyodrebnia dane użytkownika

        java.util.List<String> roles = userPrincipal.getAuthorities().stream()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                .collect(java.util.stream.Collectors.toList());
    // Wyodrebnienie rol uzytkownika, a nastepnie zebranie wszysch rol w jedna liste za pomocą kolektora.
        // Końcowym rezultatem lista ról zawiera role klienta i role barbera
        // potrzebne to jest, zeby token zawieral role uzytkownika, aby serwer wiedzial, jaki dostep ma ta lista, a ona bedzie dolaczona do tokena w "claim"
        return Jwts.builder()
                .subject((userPrincipal.getUsername()))
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }
    /*Powyższy fragment kodu przedstawia budowe tokenu JWT. Tworzymy go przez identyfikator glowny uzytkownika, zwykle jako username
    Dalej, dodajemy dowolne dane do tokenu poprzez role, w nim zawarte sa informacje o roli uzytkownika, bez tego token musialby sprawdzac baze danych, a wtedy token jest bezuzyteczny
    Nastepnie token wyswietla obecny czas, data i czas kiedy zostal utworzony
    Nastepnie ustawiany jest czas, kiedy ten totem wygasnie. Przypisany jest czas 1 dnia
    Później, token jest podpisywany, zeby nie stworzyc plagiatu tokenu po drodze
    Na koniec wszystko jest pakowane i wysylane jako jeden string
     */

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
// Funkcja pomocnicza zamieniajaca klucz string na klucz matematyczny Potrzebne to do algorytmuHS256
    public String getUserNameFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
/* Rozpakowywanie tokenu
Sprawdza, czy token JWT jest wazny, uzywany jest podczas kazdego zadania HTTP do chronionego endpointu
Serwer musi wiedziec, czy ten token jest w porzadku

 */

    public java.util.List<String> getRolesFromJwtToken(String token) {
        // Wyciągnij role bezpośrednio z claim'a tokenu JWT
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

    public java.util.Map<String, Object> getClaimsFromJwtToken(String token) {
        // Zwróć wszystkie claims z tokenu JWT
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
    /*
    Konstrukcja SWITCH case oparta na nastepujacych warunkach
    1. Czy podpis jest poprawny
    2. Czy token nie wygasl?
    3. Czy format jest prawidlowy?
    4. Czy wszystkie pola istnieja?
    Jakikolwiek test, ktory nie przejdzie, zostanie wyrzucony jako wyjatek
     */
}
