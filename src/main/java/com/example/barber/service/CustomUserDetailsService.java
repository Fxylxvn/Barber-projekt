package com.example.barber.service;

import com.example.barber.model.User;
import com.example.barber.repo.UserRepo;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Serwis dostarczający dane użytkownika na potrzeby Spring Security.
 *
 * <p>Implementuje interfejs {@link UserDetailsService} wymagany przez Spring Security
 * do uwierzytelniania użytkowników. Metoda {@link #loadUserByUsername(String)} jest
 * wywoływana przez framework podczas:
 * <ul>
 *   <li>logowania przez formularz (sesja webowa),</li>
 *   <li>walidacji tokenu JWT w filtrze {@link com.example.barber.security.AuthTokenFilter}.</li>
 * </ul>
 * </p>
 *
 * <p>Na podstawie loginu pobiera użytkownika z bazy i opakowuje go w obiekt
 * {@link UserDetails} rozumiany przez Spring Security (z rolą, hasłem, itp.).</p>
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    /** Repozytorium użytkowników – dostęp do bazy danych. */
    private final UserRepo userRepo;

    /**
     * Konstruktor wstrzykujący repozytorium użytkowników.
     *
     * @param userRepo repozytorium JPA dostarczające dane użytkowników
     */
    public CustomUserDetailsService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    /**
     * Ładuje dane użytkownika na podstawie jego nazwy użytkownika (loginu).
     *
     * <p>Pobiera encję {@link User} z bazy danych i konwertuje ją na obiekt
     * {@link UserDetails} rozumiany przez Spring Security. Rola użytkownika
     * (np. {@code "KLIENT"}) jest automatycznie poprzedzona prefiksem {@code "ROLE_"}
     * przez Spring Security ({@code ROLE_KLIENT}).</p>
     *
     * @param username nazwa użytkownika do wyszukania
     * @return obiekt {@link UserDetails} z danymi użytkownika (login, hasło BCrypt, role)
     * @throws UsernameNotFoundException jeśli użytkownik o podanym loginie nie istnieje w bazie
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepo.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("Nie znaleziono użytkownika");
        }
        return org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole())
                .build();
    }
}
