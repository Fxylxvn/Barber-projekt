package com.example.barber_api.repository;

import com.example.barber_api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Interfejs dostępu do danych (Data Access Object - DAO) dla encji User.
 * Dzięki użyciu mechaniki Spring Data JPA, interfejs ten nie wymaga własnoręcznej implementacji (Spring tworzy ją w locie).
 */
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Wbudowana metoda do filtrowania zapytań SQL typu "SELECT * FROM users WHERE role = ?".
     */
    List<User> findByRole(String role);
}
