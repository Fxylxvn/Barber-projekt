package com.example.barber.repo;

import com.example.barber.model.Appointment;
import com.example.barber.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

/*
  Repozytorium JPA do operacji na encji {@link Appointment} (wizyty).

  <p>Dziedziczy z {@link JpaRepository} i automatycznie dostarcza pełny
  zestaw operacji CRUD ({@code save}, {@code findById}, {@code findAll},
  {@code deleteById} itp.) bez konieczności pisania implementacji.
  Spring Data JPA generuje zapytania SQL na podstawie nazw metod.</p>
 */
public interface AppointmentRepo extends JpaRepository<Appointment, Long> {

    /*
      Wyszukuje wszystkie wizyty danego barbera w podanym przedziale czasowym.

      <p>Używane do sprawdzania dostępności kalendarza barbera (czy termin jest wolny).</p>

      @param barber barber, którego wizyty są wyszukiwane
      @param start  początek przedziału czasowego (włącznie)
      @param end    koniec przedziału czasowego (włącznie)
      @return lista wizyt barbera mieszczących się w podanym przedziale
     */
    List<Appointment> findByBarberAndAppointmentDateBetween(User barber, LocalDateTime start, LocalDateTime end);

    /*
      Wyszukuje wszystkie wizyty danego klienta.

      @param client klient, którego wizyty są wyszukiwane
      @return lista wszystkich wizyt klienta (posortowana domyślnie przez JPA)
     */
    List<Appointment> findByClient(User client);

    /*
      Wyszukuje wszystkie wizyty przypisane do danego barbera.

      @param barber barber, którego wizyty są wyszukiwane
      @return lista wszystkich wizyt barbera
     */
    List<Appointment> findByBarber(User barber);

    /*
      Zlicza wszystkie zakończone wizyty danego klienta (zalogowanego).
      Używane do obliczania progresu programu lojalnościowego.

      @param client zalogowany klient
      @return liczba dotychczasowych wizyt klienta
     */
    long countByClient(User client);
}

