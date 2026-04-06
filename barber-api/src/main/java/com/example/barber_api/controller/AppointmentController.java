package com.example.barber_api.controller;

import com.example.barber_api.entity.Appointment;
import com.example.barber_api.repository.AppointmentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Kontroler odpowiedzialny za zarządzanie wizytami u barberów.
 * Umożliwia pobieranie listy wizyt, tworzenie nowych, 
 * zmianę ich statusu oraz ich całkowite anulowanie.
 */
@RestController
@RequestMapping("/api/appointments")
@Tag(name = "Rezerwacje", description = "Zarządzanie wizytami u barbera")
public class AppointmentController {

    private final AppointmentRepository appointmentRepository;

    public AppointmentController(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    /**
     * Zwraca listę wszystkich rezerwacji zapisanych w bazie danych.
     */
    @Operation(summary = "Pobranie wszystkich wizyt")
    @GetMapping
    public ResponseEntity<List<Appointment>> getAppointments() {
        return ResponseEntity.ok(appointmentRepository.findAll());
    }

    /**
     * Zapisuje nową wizytę przekazaną bezpośrednio w formacie JSON (Encja Appointment).
     */
    @Operation(summary = "Umówienie nowej rezerwacji")
    @PostMapping
    public ResponseEntity<Appointment> createAppointment(@RequestBody Appointment appointment) {
        appointment.setStatus("PENDING");
        Appointment saved = appointmentRepository.save(appointment);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Zmienia status istniejącej wizyty.
     */
    @Operation(summary = "Zmiana statusu rezerwacji")
    @PatchMapping("/{id}/status")
    public ResponseEntity<Appointment> updateAppointmentStatus(@PathVariable Long id, @RequestBody StatusUpdateRequest request) {
        return appointmentRepository.findById(id).map(a -> {
            a.setStatus(request.getStatus());
            Appointment saved = appointmentRepository.save(a);
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    public static class StatusUpdateRequest {
        private String status;
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    /**
     * Trwale usuwa rezerwację z bazy danych po podanym ID.
     */
    @Operation(summary = "Usunięcie wizyty")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAppointment(@PathVariable Long id) {
        appointmentRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
