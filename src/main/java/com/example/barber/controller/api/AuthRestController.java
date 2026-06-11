package com.example.barber.controller.api;

import com.example.barber.dto.JwtResponse;
import com.example.barber.dto.LoginRequest;
import com.example.barber.model.User;
import com.example.barber.repo.UserRepo;
import com.example.barber.security.JwtUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Kontroler REST obsługujący uwierzytelnianie i rejestrację użytkowników przez API.
 *
 * <p>Dostępny pod ścieżką bazową {@code /api/auth}. Wszystkie endpointy są publiczne
 * (nie wymagają wcześniejszego zalogowania) – patrz konfiguracja w
 * {@link com.example.barber.config.SecurityConfig}.</p>
 *
 * <p>Obsługuje dwa przepływy:
 * <ul>
 *   <li><b>Logowanie</b> ({@code POST /api/auth/login}) – weryfikacja hasła i zwrot tokenu JWT.</li>
 *   <li><b>Rejestracja</b> ({@code POST /api/auth/register}) – tworzenie nowego konta klienta.</li>
 * </ul>
 * </p>
 *
 * <p>{@code @CrossOrigin(origins = "*")} pozwala na wywołania z dowolnej domeny
 * (przydatne podczas developmentu frontendu).</p>
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthRestController {

    /** Menedżer uwierzytelniania Spring Security – weryfikuje login i hasło. */
    private final AuthenticationManager authenticationManager;

    /** Repozytorium użytkowników – dostęp do bazy danych. */
    private final UserRepo userRepo;

    /** Narzędzie do generowania i walidacji tokenów JWT. */
    private final JwtUtils jwtUtils;

    /**
     * Konstruktor wstrzykujący wymagane zależności przez Spring.
     *
     * @param authenticationManager menedżer uwierzytelniania Spring Security
     * @param userRepo              repozytorium użytkowników
     * @param jwtUtils              narzędzie JWT
     */
    public AuthRestController(AuthenticationManager authenticationManager, UserRepo userRepo, JwtUtils jwtUtils) {
        this.authenticationManager = authenticationManager;
        this.userRepo = userRepo;
        this.jwtUtils = jwtUtils;
    }

    /**
     * Uwierzytelnia użytkownika i zwraca token JWT.
     *
     * <p>Przepływ:
     * <ol>
     *   <li>Spring Security weryfikuje login i hasło BCrypt.</li>
     *   <li>Po sukcesie generowany jest token JWT zawierający login i role.</li>
     *   <li>Token jest zwracany w obiekcie {@link JwtResponse} (JSON).</li>
     * </ol>
     * Klient musi dołączyć otrzymany token do kolejnych żądań
     * w nagłówku {@code Authorization: Bearer <token>}.</p>
     *
     * @param loginRequest ciało żądania z polami {@code username} i {@code password}
     * @return {@code 200 OK} z {@link JwtResponse} zawierającym token, login i rolę
     */
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        User user = userRepo.findByUsername(loginRequest.getUsername());
        return ResponseEntity.ok(new JwtResponse(jwt, user.getUsername(), user.getRole()));
    }

    /**
     * Rejestruje nowego użytkownika w systemie z rolą KLIENT.
     *
     * <p>Jeśli podana nazwa użytkownika jest już zajęta, zwracany jest błąd 400.
     * Nowo zarejestrowany użytkownik ma automatycznie przypisaną rolę {@code "KLIENT"}.</p>
     *
     * <p><b>Uwaga:</b> Hasło jest tutaj zapisywane bez hashowania – w środowisku produkcyjnym
     * należy dodać kodowanie BCrypt przed zapisem.</p>
     *
     * @param user obiekt użytkownika deserializowany z ciała żądania JSON
     * @return {@code 200 OK} z komunikatem sukcesu lub {@code 400 Bad Request}
     *         jeśli nazwa użytkownika jest zajęta
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        if (userRepo.findByUsername(user.getUsername()) != null) {
            return ResponseEntity.badRequest().body("Error: Username is already taken!");
        }

        user.setRole("KLIENT"); // Default role
        userRepo.save(user);

        return ResponseEntity.ok("User registered successfully!");
    }
}
