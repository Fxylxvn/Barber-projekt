package com.example.barber.repo;

import com.example.barber.model.Appointment;
import com.example.barber.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepo extends JpaRepository<Appointment, Long> {
    List<Appointment> findByBarberAndAppointmentDateBetween(User barber, LocalDateTime start, LocalDateTime end);
    List<Appointment> findByClient(User client);
    List<Appointment> findByBarber(User barber);
}


