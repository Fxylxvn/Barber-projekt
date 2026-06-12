package com.example.barber.config;

import com.example.barber.security.AuthTokenFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/*
  Główna konfiguracja bezpieczeństwa aplikacji (Spring Security).

  <p>Definiuje:
 * <ul>
 *   <li>Reguły dostępu do poszczególnych ścieżek URL (kto może co wywołać).</li>
 *   <li>Formularz logowania i wylogowania dla widoków webowych (Thymeleaf).</li>
 *   <li>Integrację filtra JWT ({@link AuthTokenFilter}) do obsługi zapytań REST API.</li>
 *   <li>Koder haseł BCrypt używany do hashowania i weryfikacji haseł.</li>
 * </ul>
 * </p>
 *
 * <p>Aplikacja używa podwójnego mechanizmu uwierzytelniania:
 * <ul>
 *   <li><b>Sesja webowa</b> – dla widoków HTML (formularz logowania pod {@code /login}).</li>
 *   <li><b>Token JWT</b> – dla zapytań REST API (nagłówek {@code Authorization: Bearer <token>}).</li>
 * </ul>
 * </p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Filtr JWT wstrzykiwany przez Spring – dodawany do łańcucha filtrów.
    private final AuthTokenFilter authTokenFilter;

    /*
      Konstruktor wstrzykujący filtr JWT.

      @param authTokenFilter filtr sprawdzający tokeny JWT w nagłówkach żądań
     */
    public SecurityConfig(AuthTokenFilter authTokenFilter) {
        this.authTokenFilter = authTokenFilter;
    }

    /*
      Udostępnia {@link AuthenticationManager} jako bean Springa.

      <p>Używany w {@link com.example.barber.controller.api.AuthRestController}
      do programowego uwierzytelniania użytkownika na podstawie loginu i hasła.</p>
      @param authConfig konfiguracja uwierzytelniania dostarczana przez Spring Security
      @return instancja {@link AuthenticationManager}
      @throws Exception jeśli nie uda się zbudować menedżera
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /*
      Konfiguruje łańcuch filtrów bezpieczeństwa HTTP.

      <p>Reguły dostępu:
      <ul>
        <li>{@code /api/auth/**} – publiczny (logowanie i rejestracja przez API).</li>
        <li>{@code /login, /register, /css/**, /js/**, /img/**, /barber-info/**, /uploads/**}
            – publiczne (strony i zasoby statyczne).</li>
        <li>{@code /api/users/**} – dla każdej zalogowanej roli.</li>
        <li>{@code /api/tasks/**} – tylko ADMIN, MANAGER, BARBER.</li>
        <li>{@code /api/reservations/**} – ADMIN, MANAGER, BARBER, KLIENT.</li>
        <li>{@code /barber/**} – wyłącznie BARBER.</li>
        <li>{@code /client/**} – wyłącznie KLIENT.</li>
        <li>Wszystkie pozostałe żądania – wymagają zalogowania.</li>
      </ul>
      Filtr JWT jest wstawiany przed {@code UsernamePasswordAuthenticationFilter},
      dzięki czemu tokeny API są sprawdzane przed logowaniem sesyjnym.</p>

      @param http budowniczy konfiguracji HTTP Security dostarczany przez Spring
      @return zbudowany łańcuch filtrów {@link SecurityFilterChain}
      @throws Exception w przypadku błędu konfiguracji
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/login", "/register", "/css/**", "/js/**", "/img/**", "/barber-info/**", "/uploads/**").permitAll()
                        .requestMatchers("/api/users/**").hasAnyRole("ADMIN", "MANAGER", "USER", "BARBER", "KLIENT")
                        .requestMatchers("/api/tasks/**").hasAnyRole("ADMIN", "MANAGER", "BARBER")
                        .requestMatchers("/api/reservations/**").hasAnyRole("ADMIN", "MANAGER", "BARBER", "KLIENT")
                        .requestMatchers("/api/chatbot/**").hasRole("KLIENT")
                        .requestMatchers("/barber/**").hasRole("BARBER")
                        .requestMatchers("/client/**").hasRole("KLIENT")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login")
                        .permitAll()
                )
                .exceptionHandling(exception -> exception
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.sendRedirect("/");
                        })
                )
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()));

        // Dodaj filtr JWT przed standardowym filtrem logowania
        http.addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /*
      Tworzy bean kodera haseł oparty na algorytmie BCrypt.

      <p>BCrypt automatycznie dodaje sól i jest odporny na ataki brute-force.
      Używany zarówno przy rejestracji (hashowanie hasła) jak i logowaniu
      (weryfikacja hasła wprowadzonego przez użytkownika).</p>

      @return instancja {@link BCryptPasswordEncoder}
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
