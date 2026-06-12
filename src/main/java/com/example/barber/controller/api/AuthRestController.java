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

/*
  Kontroler REST obsługujący uwierzytelnianie i rejestrację użytkowników przez API.

  Dostępny pod ścieżką bazową {@code /api/auth}. Wszystkie endpointy są publiczne
  (nie wymagają wcześniejszego zalogowania)

 Obsługuje dwa przepływy:

    Logowanie ({@code POST /api/auth/login}) – weryfikacja hasła i zwrot tokenu JWT.
    <Rejestracja({@code POST /api/auth/register}) – tworzenie nowego konta klienta.

  {@code @CrossOrigin(origins = "*")} pozwala na wywołania z dowolnej domeny
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthRestController {

    private final AuthenticationManager authenticationManager;

    // Repozytorium użytkowników – dostęp do bazy danych.
    private final UserRepo userRepo;

    // Narzędzie do generowania i walidacji tokenów JWT.
    private final JwtUtils jwtUtils;

    /*
      Konstruktor wstrzykujący wymagane zależności przez Spring.

      @param authenticationManager menedżer uwierzytelniania Spring Security
      @param userRepo              repozytorium użytkowników
      @param jwtUtils              narzędzie JWT
     */
    public AuthRestController(AuthenticationManager authenticationManager, UserRepo userRepo, JwtUtils jwtUtils) {
        this.authenticationManager = authenticationManager;
        this.userRepo = userRepo;
        this.jwtUtils = jwtUtils;
    }

    /*
      Uwierzytelnia użytkownika i zwraca token JWT.

     Przepływ:

       Spring Security weryfikuje login i hasło BCrypt.
       Po sukcesie generowany jest token JWT zawierający login i rle.
       Token jest zwracany w obiekcie {@link JwtResponse} (JSON).

      Klient musi dołączyć otrzymany token do kolejnych żądań
      w nagłówku {@code Authorization: Bearer <token>}.

      @param loginRequest ciało żądania z polami {@code username} i {@code password}
      @return {@code 200 OK} z {@link JwtResponse} zawierającym token, login i rolę
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

    /*
      Rejestruje nowego użytkownika w systemie z rolą KLIENT.

      Jeśli podana nazwa użytkownika jest już zajęta, zwracany jest błąd 400.
      Nowo zarejestrowany użytkownik ma automatycznie przypisaną rolę {@code "KLIENT"}.

      Hasło jest tutaj zapisywane bez hashowania – w środowisku produkcyjnym
      należy dodać kodowanie BCrypt przed zapisem.

      @param user obiekt użytkownika deserializowany z ciała żądania JSON
      @return {@code 200 OK} z komunikatem sukcesu lub {@code 400 Bad Request}
      jeśli nazwa użytkownika jest zajęta
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        if (userRepo.findByUsername(user.getUsername()) != null) {
            return ResponseEntity.badRequest().body("Error: Username is already taken!");
        }

        user.setRole("KLIENT"); // Rola domyślna
        userRepo.save(user);

        return ResponseEntity.ok("User registered successfully!");
    }
}
