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

@RestController
@RequestMapping("/api/demo")
public class TokenDemoController {

    private final JwtUtils jwtUtils;

    @Value("${barber.app.jwtSecret:myVerySecretKeyForBarberAppThatIsAtLeast32CharactersLong}")
    private String jwtSecret;

    public TokenDemoController(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @GetMapping("/verify-token")
    public ResponseEntity<?> verifyToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("Brak poprawnego nagłówka Authorization");
        }

        String token = authHeader.substring(7);

        try {
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

            // Możemy również zweryfikować czy użytkownik ma kontekst bezpieczeństwa po przejściu przez filtr
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
