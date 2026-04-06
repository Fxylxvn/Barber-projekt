package com.example.barber_api.repository;

import com.example.barber_api.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Abstrakcja pozwalająca łączyć rezerwacje wizyt po stronie kontrolerów 
 * bezpośrednio z mapowaniem w bazie SQL (z automatycznym wykrywaniem transakcji).
 */
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
}
