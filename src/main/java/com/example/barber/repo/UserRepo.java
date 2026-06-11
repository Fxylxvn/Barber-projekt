package com.example.barber.repo;

import com.example.barber.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Repozytorium JPA do operacji na encji {@link User} (użytkownicy).
 *
 * <p>Dziedziczy z {@link JpaRepository} i automatycznie dostarcza pełny
 * zestaw operacji CRUD ({@code save}, {@code findById}, {@code findAll},
 * {@code deleteById} itp.) bez konieczności pisania implementacji.
 * Spring Data JPA generuje zapytania SQL na podstawie nazw metod.</p>
 */
public interface UserRepo extends JpaRepository<User, Long> {

    /**
     * Wyszukuje użytkownika po nazwie użytkownika (loginie).
     *
     * <p>Używane m.in. przez {@link com.example.barber.service.CustomUserDetailsService}
     * podczas uwierzytelniania i przez kontrolery do pobierania danych zalogowanego użytkownika.</p>
     *
     * @param username nazwa użytkownika (login)
     * @return obiekt {@link User} lub {@code null}, jeśli użytkownik nie istnieje
     */
    User findByUsername(String username);

    /**
     * Wyszukuje wszystkich użytkowników z podaną rolą.
     *
     * <p>Używane np. do pobierania listy wszystkich barberów ({@code "BARBER"})
     * lub klientów ({@code "KLIENT"}) na potrzeby widoków i logiki biznesowej.</p>
     *
     * @param role rola do filtrowania, np. {@code "BARBER"} lub {@code "KLIENT"}
     * @return lista użytkowników z daną rolą
     */
    List<User> findByRole(String role);
}
