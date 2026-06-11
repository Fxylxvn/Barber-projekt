package com.example.barber.controller.api;

import com.example.barber.model.Appointment;
import com.example.barber.model.User;
import com.example.barber.repo.AppointmentRepo;
import com.example.barber.repo.UserRepo;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Kontroler REST obsługujący operacje na rezerwacjach (wizytach) przez API.
 *
 * <p>Dostępny pod ścieżką bazową {@code /api/reservations}. Wymaga zalogowania
 * (rola KLIENT, BARBER, MANAGER lub ADMIN) – patrz konfiguracja w
 * {@link com.example.barber.config.SecurityConfig}.</p>
 *
 * <p>Implementuje trzy operacje:
 * <ul>
 *   <li><b>GET</b> – pobranie listy rezerwacji (filtrowanej według roli użytkownika).</li>
 *   <li><b>POST</b> – utworzenie nowej rezerwacji.</li>
 *   <li><b>DELETE</b> – usunięcie rezerwacji (z kontrolą uprawnień).</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/api/reservations")
public class ReservationRestController {

    /** Repozytorium wizyt – operacje CRUD na encji {@link Appointment}. */
    private final AppointmentRepo appointmentRepo;

    /** Repozytorium użytkowników – pobieranie danych zalogowanego użytkownika. */
    private final UserRepo userRepo;

    /**
     * Konstruktor wstrzykujący wymagane repozytoria przez Spring.
     *
     * @param appointmentRepo repozytorium wizyt
     * @param userRepo        repozytorium użytkowników
     */
    public ReservationRestController(AppointmentRepo appointmentRepo, UserRepo userRepo) {
        this.appointmentRepo = appointmentRepo;
        this.userRepo = userRepo;
    }

    /**
     * Pobiera listę rezerwacji dostosowaną do roli zalogowanego użytkownika.
     *
     * <p>Logika dostępu:
     * <ul>
     *   <li>ADMIN / MANAGER – widzi wszystkie rezerwacje w systemie.</li>
     *   <li>BARBER – widzi tylko wizyty przypisane do siebie.</li>
     *   <li>KLIENT – widzi tylko swoje własne wizyty.</li>
     * </ul>
     * </p>
     *
     * @param auth dane uwierzytelnienia zalogowanego użytkownika (wstrzykiwane przez Spring)
     * @return lista rezerwacji dopasowana do roli użytkownika
     */
    @GetMapping
    public List<Appointment> getReservations(Authentication auth) {
        User user = userRepo.findByUsername(auth.getName());
        if (user.getRole().equals("ADMIN") || user.getRole().equals("MANAGER")) {
            return appointmentRepo.findAll();
        }
        if (user.getRole().equals("BARBER")) {
            return appointmentRepo.findByBarber(user);
        }
        return appointmentRepo.findByClient(user);
    }

    /**
     * Tworzy nową rezerwację (wizytę) w systemie.
     *
     * <p>Jeśli pole {@code client} w ciele żądania jest puste, zostaje automatycznie
     * przypisany zalogowany użytkownik. Barber musi być podany jawnie – jeśli go brak,
     * zwracany jest błąd 400.</p>
     *
     * @param appointment obiekt rezerwacji deserializowany z ciała żądania JSON
     * @param auth        dane uwierzytelnienia zalogowanego użytkownika
     * @return {@code 200 OK} z zapisanym obiektem {@link Appointment} lub
     *         {@code 400 Bad Request} jeśli nie podano barbera
     */
    @PostMapping
    public ResponseEntity<?> createReservation(@RequestBody Appointment appointment, Authentication auth) {
        User user = userRepo.findByUsername(auth.getName());
        if (appointment.getClient() == null) {
            appointment.setClient(user);
        }
        if (appointment.getBarber() == null) {
            return ResponseEntity.badRequest().body("Error: Barber must be specified!");
        }
        // Basic validation or business logic could go here
        return ResponseEntity.ok(appointmentRepo.save(appointment));
    }

    /**
     * Usuwa rezerwację o podanym identyfikatorze.
     *
     * <p>Usunięcie jest możliwe tylko jeśli zalogowany użytkownik jest:
     * <ul>
     *   <li>Administratorem (rola ADMIN), lub</li>
     *   <li>Klientem, który tę wizytę zarezerwował, lub</li>
     *   <li>Barberem przypisanym do tej wizyty.</li>
     * </ul>
     * W przeciwnym razie zwracany jest błąd {@code 403 Forbidden}.</p>
     *
     * @param id   identyfikator rezerwacji do usunięcia
     * @param auth dane uwierzytelnienia zalogowanego użytkownika
     * @return {@code 204 No Content} po sukcesie, {@code 403 Forbidden} przy braku uprawnień,
     *         {@code 404 Not Found} jeśli rezerwacja nie istnieje
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReservation(@PathVariable("id") Long id, Authentication auth) {
        return appointmentRepo.findById(id).map(appointment -> {
            User user = userRepo.findByUsername(auth.getName());
            // Check if user has permission to delete (Admin or owner)
            if (user.getRole().equals("ADMIN") ||
                appointment.getClient().getId().equals(user.getId()) ||
                appointment.getBarber().getId().equals(user.getId())) {
                appointmentRepo.deleteById(id);
                return ResponseEntity.noContent().<Void>build();
            }
            return ResponseEntity.status(403).<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
