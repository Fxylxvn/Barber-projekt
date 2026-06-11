package com.example.barber.repo;

import com.example.barber.model.Task;
import com.example.barber.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repozytorium JPA do operacji na encji {@link Task} (zadania).
 *
 * <p>Dziedziczy z {@link JpaRepository} i automatycznie dostarcza pełny
 * zestaw operacji CRUD bez konieczności pisania implementacji.
 * Spring Data JPA generuje zapytania SQL na podstawie nazw metod.</p>
 */
@Repository
public interface TaskRepo extends JpaRepository<Task, Long> {

    /**
     * Wyszukuje wszystkie zadania przypisane do danego użytkownika.
     *
     * @param user użytkownik, do którego przypisano zadania
     * @return lista zadań przypisanych do użytkownika
     */
    List<Task> findByAssignedTo(User user);

    /**
     * Wyszukuje wszystkie zadania oznaczone jako publiczne ({@code isPublic = true}).
     *
     * <p>Zadania publiczne są widoczne dla wszystkich uprawnionych użytkowników,
     * niezależnie od przypisania.</p>
     *
     * @return lista wszystkich publicznych zadań
     */
    List<Task> findByIsPublicTrue();
}
