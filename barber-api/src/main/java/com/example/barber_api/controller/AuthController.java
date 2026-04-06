package com.example.barber_api.controller;

import com.example.barber_api.entity.User;
import com.example.barber_api.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * Kontroler obsługujący operacje autoryzacyjne.
 * Zarządza procesem rejestracji oraz logowania użytkowników do systemu.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autoryzacja", description = "Endpointy autoryzacyjne")
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Wymaga przesłania całego obiektu User.
     * Dodaje go do bazy z domyślną rolą klienta.
     */
    @Operation(summary = "Rejestracja nowego klienta")
    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody User user) {
        user.setRole("CLIENT");
        User savedUser = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    /**
     * Prosta weryfikacja logowania (Mock).
     */
    @Operation(summary = "Logowanie do systemu")
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest request) {
        // Zwracamy po prostu udawany token dla pokazu
        return ResponseEntity.ok(Map.of("token", "mocked-jwt", "role", "CLIENT"));
    }

    public static class LoginRequest {
        private String email;
        private String password;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
