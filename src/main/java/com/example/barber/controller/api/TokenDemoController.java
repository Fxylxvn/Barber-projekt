package com.example.barber.controller.api;

import com.example.barber.security.JwtUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/*
  Kontroler REST służący do demonstracji i diagnostyki mechanizmu JWT.

  <p>Dostępny pod ścieżką bazową {@code /api/demo}. Umożliwia ręczne
  sprawdzenie (zweryfikowanie) tokenu JWT – przydatne podczas developmentu
  i uczenia się działania systemu autoryzacji.</p>

  <p>Endpoint odczytuje token z nagłówka HTTP, weryfikuje jego podpis i datę ważności,
  a następnie zwraca wszystkie zawarte w nim informacje (username, role, daty)
  w formacie JSON.</p>

  <p><b>Uwaga:</b> Ten kontroler jest przeznaczony wyłącznie do celów demonstracyjnych
  i nie powinien być eksponowany w środowisku produkcyjnym bez dodatkowego zabezpieczenia.</p>
 */
@RestController
@RequestMapping("/api/demo")
public class TokenDemoController {

    // Narzędzie do operacji na tokenach JWT.
    private final JwtUtils jwtUtils;

    /*
      Tajny klucz JWT wczytywany z konfiguracji aplikacji.
      Musi być identyczny z kluczem używanym podczas generowania tokenów.
     */
    @Value("${barber.app.jwtSecret:myVerySecretKeyForBarberAppThatIsAtLeast32CharactersLong}")
    private String jwtSecret;

    /*
      Konstruktor wstrzykujący narzędzie JWT przez Spring.
      @param jwtUtils narzędzie do obsługi tokenów JWT
     */
    public TokenDemoController(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    /*
      Weryfikuje token JWT przesłany w nagłówku i zwraca jego zawartość.

      <p>Nagłówek musi mieć format: {@code Authorization: Bearer <token>}.
      W odpowiedzi zwracane są:
      <ul>
        <li>{@code message} – komunikat potwierdzający poprawność tokenu.</li>
        <li>{@code username} – nazwa użytkownika zapisana w tokenie.</li>
        <li>{@code roles} – lista ról użytkownika z tokenu.</li>
        <li>{@code expiration} – data i czas wygaśnięcia tokenu.</li>
        <li>{@code authenticated_user} – użytkownik z kontekstu Spring Security (jeśli dostępny).</li>
        <li>{@code authorities} – uprawnienia z kontekstu Spring Security.</li>
      </ul>
      </p>

      @param authHeader zawartość nagłówka {@code Authorization}
      @return {@code 200 OK} z mapą danych tokenu lub
              {@code 400 Bad Request} jeśli nagłówek jest nieprawidłowy lub token nieważny
     */
    @GetMapping("/verify-token")
    public ResponseEntity<?> verifyToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("Brak poprawnego nagłówka Authorization");
        }

        // Usuń prefix "Bearer " i zostaw sam token
        String token = authHeader.substring(7);

        try {
            // Parsuj i weryfikuj token przy użyciu tajnego klucza
            Claims claims = Jwts.parser()
                    .verifyWith((javax.crypto.SecretKey) Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Token jest poprawny");
            response.put("username", claims.getSubject());
            response.put("roles", claims.get("roles"));
            response.put("expiration", claims.getExpiration());

            // Pobierz też informacje z kontekstu Spring Security (ustawione przez filtr JWT)
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                response.put("authenticated_user", authentication.getName());
                response.put("authorities", authentication.getAuthorities());
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Błąd weryfikacji tokenu: " + e.getMessage());
        }
    }
}
