package com.example.barber.controller.api;

import com.example.barber.model.User;
import com.example.barber.repo.UserRepo;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
  Kontroler REST obsługujący operacje na użytkownikach przez API.

  <p>Dostępny pod ścieżką bazową {@code /api/users}. Różne operacje wymagają
  różnych ról:
  <ul>
    <li>Pobranie listy wszystkich użytkowników – tylko ADMIN lub MANAGER.</li>
    <li>Pobranie i aktualizacja konkretnego użytkownika – każda zalogowana rola
        (KLIENT, BARBER, ADMIN, MANAGER).</li>
    <li>Usunięcie użytkownika – tylko ADMIN.</li>
  </ul>
  </p>
 */
@RestController
@RequestMapping("/api/users")
public class UserRestController {

    // Repozytorium użytkowników – operacje CRUD na encji {@link User}.
    private final UserRepo userRepo;

    // Koder haseł BCrypt – używany do hashowania nowego hasła przy aktualizacji.
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    /*
      Konstruktor wstrzykujący wymagane zależności przez Spring.
      @param userRepo        repozytorium użytkowników
      @param passwordEncoder koder haseł BCrypt
     */
    public UserRestController(UserRepo userRepo, org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    /*
      Pobiera listę wszystkich użytkowników w systemie.

      <p>Dostęp ograniczony do ról ADMIN i MANAGER za pomocą adnotacji
      {@code @PreAuthorize}.</p>
      @return lista wszystkich encji {@link User}
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    /*
      Pobiera dane użytkownika o podanym identyfikatorze.

      @param id identyfikator użytkownika
      @return {@code 200 OK} z obiektem {@link User} lub
              {@code 404 Not Found} jeśli użytkownik nie istnieje
     */
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable("id") Long id) {
        return userRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /*
      Aktualizuje dane użytkownika o podanym identyfikatorze.

      <p>Aktualizowane pola: {@code name}, {@code username}.
      Hasło jest zmieniane tylko wtedy, gdy zostanie przekazane i jest niepuste –
      w takim przypadku jest automatycznie hashowane BCryptem przed zapisem.</p>

      @param id          identyfikator użytkownika do aktualizacji
      @param userDetails nowe dane użytkownika deserializowane z ciała żądania JSON
      @return {@code 200 OK} z zaktualizowanym obiektem {@link User} lub
              {@code 404 Not Found} jeśli użytkownik nie istnieje
     */
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable("id") Long id, @RequestBody User userDetails) {
        return userRepo.findById(id).map(user -> {
            user.setName(userDetails.getName());
            user.setUsername(userDetails.getUsername());
            // Zaktualizuj hasło tylko jeśli zostało przesłane (hashuj BCrypt)
            if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
                user.setPassword(passwordEncoder.encode(userDetails.getPassword()));
            }
            return ResponseEntity.ok(userRepo.save(user));
        }).orElse(ResponseEntity.notFound().build());
    }

    /*
      Usuwa użytkownika o podanym identyfikatorze.

      <p>Dostęp ograniczony wyłącznie do roli ADMIN za pomocą adnotacji
      {@code @PreAuthorize}.</p>

      @param id identyfikator użytkownika do usunięcia
      @return {@code 204 No Content} po sukcesie lub
              {@code 404 Not Found} jeśli użytkownik nie istnieje
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") Long id) {
        if (userRepo.existsById(id)) {
            userRepo.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
