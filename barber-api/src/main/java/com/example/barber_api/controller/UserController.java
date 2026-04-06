package com.example.barber_api.controller;

import com.example.barber_api.entity.User;
import com.example.barber_api.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Koncentruje endpointy związane z przeglądaniem innych ludzi w systemie.
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "Użytkownicy", description = "Endpointy zarządzania użytkownikami")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Zwraca encje User powiązane z Barberami.
     */
    @Operation(summary = "Lista dostępnych barberów")
    @GetMapping("/barbers")
    public ResponseEntity<List<User>> getBarbers() {
        return ResponseEntity.ok(userRepository.findByRole("BARBER"));
    }

    /**
     * Zwraca przykładowy profil użytkownika dla pokazu zasobów chronionych.
     */
    @Operation(summary = "Pobranie własnego profilu")
    @GetMapping("/me")
    public ResponseEntity<User> getMyProfile() {
        return userRepository.findById(4L) // Na twardo wpisane ID Klienta z DataInitializer dla prezentacji
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Aktualizuje imię, nazwisko oraz numer w zadanym profilu.
     */
    @Operation(summary = "Aktualizacja profilu")
    @PutMapping("/me")
    public ResponseEntity<User> updateMyProfile(@RequestBody User profileData) {
        // Zapisuje zmienione dane dla testowego klienta
        return userRepository.findById(4L).map(u -> {
            u.setFirstName(profileData.getFirstName());
            u.setLastName(profileData.getLastName());
            u.setPhone(profileData.getPhone());
            return ResponseEntity.ok(userRepository.save(u));
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Pobiera wszystko operując bezpośrednio na tabelach.
     */
    @Operation(summary = "Lista wszystkich użytkowników")
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers(@RequestParam(required = false) String role) {
        List<User> list = (role != null) ? userRepository.findByRole(role) : userRepository.findAll();
        return ResponseEntity.ok(list);
    }
}
