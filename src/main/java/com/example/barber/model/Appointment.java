package com.example.barber.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/*
  Encja reprezentująca wizytę (rezerwację) w salonie barberskim.

  <p>Przechowywana w tabeli {@code appointment}.
  Każda wizyta łączy klienta ({@link User} z rolą KLIENT) lub gościa
  z barberem ({@link User} z rolą BARBER) na określony termin i rodzaj usługi.</p>

  <p>Jeśli {@code isGuest = true}, pola {@code guestName}, {@code guestEmail}
  i {@code guestPhone} zawierają dane gościa, a {@code client} jest null.</p>

  <p>Pole {@code priceCharged} przechowuje faktyczną kwotę do zapłaty
  (uwzględniając zniżkę lojalnościową lub dopłatę gościa).</p>
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
      Klient, który zarezerwował wizytę (null dla gości niezalogowanych).
      Relacja wiele-do-jednego: wielu klientów może mieć wiele wizyt.
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

    // ── Pola systemu cen i gości ─────────────────────────────────────────────

    /*
      Flaga wskazująca, czy wizyta jest zarezerwowana przez gościa (niezalogowanego).
      Gdy {@code true} – używane są pola guestName, guestEmail, guestPhone.
     */
    private boolean isGuest = false;

    // Imię i nazwisko gościa (wypełniane tylko gdy isGuest = true).
    private String guestName;

    // Adres e-mail gościa do potwierdzenia rezerwacji.
    private String guestEmail;

    // Numer telefonu gościa do kontaktu.
    private String guestPhone;

    /*
      Faktyczna cena naliczona za wizytę w złotych (PLN).
      Dla gości: cena podstawowa + 25% dopłata.
      Dla zalogowanych: cena podstawowa.
      Dla wizyt lojalnościowych (co 5. wizyta): cena podstawowa - 30%.
     */
    private Integer priceCharged;

    /*
      Flaga informująca, czy zastosowano zniżkę lojalnościową (-30%).
      Używana do wyświetlenia informacji na dashboardzie.
     */
    private boolean discountApplied = false;
}
