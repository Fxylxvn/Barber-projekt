package com.example.barber.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/*
  Encja reprezentująca wizytę (rezerwację) w salonie barberskim.

  <p>Przechowywana w tabeli {@code appointment} (nazwa domyślna JPA).
  Każda wizyta łączy klienta ({@link User} z rolą KLIENT) z barberem
  ({@link User} z rolą BARBER) na określony termin i rodzaj usługi.</p>

  <p>Lombok {@code @Data} generuje gettery, settery, {@code toString},
  {@code equals} i {@code hashCode}.</p>
 */
@Entity
@Data
public class Appointment {

    // Unikalny identyfikator wizyty, generowany automatycznie przez bazę danych.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Data i godzina zaplanowanej wizyty.
    private LocalDateTime appointmentDate;

    /*
      Klient, który zarezerwował wizytę.
      Relacja wiele-do-jednego: wielu klientów może mieć wiele wizyt, ale każda wizyta
      należy do jednego klienta.
     */
    @ManyToOne
    private User client;

    /*
      Barber obsługujący wizytę.
      Relacja wiele-do-jednego: jeden barber może mieć wiele wizyt.
     */
    @ManyToOne
    private User barber;

    // Krótki opis wizyty (zazwyczaj taki sam jak {@code serviceType}).
    private String description;

    // Rodzaj usługi, np. "Włosy", "Broda", "Włosy + Broda", "Wszystko".
    private String serviceType;

    // Czas trwania usługi w minutach (zależy od wybranego rodzaju usługi).
    private Integer durationMinutes;

    // Dodatkowe uwagi klienta do wizyty (max 1000 znaków).
    @Column(length = 1000)
    private String notes;
}
