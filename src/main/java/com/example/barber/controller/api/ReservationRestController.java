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

@RestController
@RequestMapping("/api/reservations")
public class ReservationRestController {

    private final AppointmentRepo appointmentRepo;
    private final UserRepo userRepo;

    public ReservationRestController(AppointmentRepo appointmentRepo, UserRepo userRepo) {
        this.appointmentRepo = appointmentRepo;
        this.userRepo = userRepo;
    }

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
