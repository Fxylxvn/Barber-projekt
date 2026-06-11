package com.example.barber.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtr HTTP odpowiedzialny za uwierzytelnianie żądań za pomocą tokenu JWT.
 *
 * <p>Wykonywany raz dla każdego przychodzącego żądania HTTP (dziedziczy z
 * {@link OncePerRequestFilter}). Jego zadanie to:
 * <ol>
 *   <li>Odczytanie tokenu JWT z nagłówka {@code Authorization: Bearer <token>}.</li>
 *   <li>Walidacja tokenu (podpis, czas ważności).</li>
 *   <li>Wyodrębnienie nazwy użytkownika z tokenu i załadowanie jego danych z bazy.</li>
 *   <li>Ustawienie uwierzytelnienia w {@link SecurityContextHolder}, dzięki czemu
 *       Spring Security traktuje żądanie jako zalogowane.</li>
 * </ol>
 * Filtr jest wstrzykiwany do łańcucha filtrów bezpieczeństwa w {@link com.example.barber.config.SecurityConfig}
 * przed filtrem {@code UsernamePasswordAuthenticationFilter}.</p>
 */
@Component
public class AuthTokenFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);

    /** Narzędzia JWT: generowanie, walidacja i odczyt danych z tokenu. */
    private final JwtUtils jwtUtils;

    /** Serwis ładujący szczegóły użytkownika (role, hasło) z bazy danych. */
    private final UserDetailsService userDetailsService;

    /**
     * Konstruktor wstrzykujący wymagane zależności przez Spring.
     *
     * @param jwtUtils           narzędzie do operacji na tokenach JWT
     * @param userDetailsService serwis dostarczający dane użytkownika na podstawie loginu
     */
    public AuthTokenFilter(JwtUtils jwtUtils, UserDetailsService userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Główna logika filtra – wywoływana raz dla każdego żądania HTTP.
     *
     * <p>Jeśli w nagłówku {@code Authorization} znajduje się ważny token JWT,
     * metoda ustawia uwierzytelnienie w kontekście Spring Security, umożliwiając
     * dostęp do chronionych zasobów. W przypadku braku lub błędnego tokenu
     * żądanie przechodzi dalej bez ustawiania uwierzytelnienia (Spring Security
     * odrzuci je samodzielnie, jeśli zasób tego wymaga).</p>
     *
     * @param request     przychodzące żądanie HTTP
     * @param response    odpowiedź HTTP
     * @param filterChain łańcuch filtrów – wywołanie {@code doFilter} przekazuje
     *                    żądanie do następnego filtra lub do kontrolera
     * @throws ServletException w przypadku błędu serwletu
     * @throws IOException      w przypadku błędu wejścia/wyjścia
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // Pobierz token z nagłówka Authorization
            String jwt = parseJwt(request);

            // Jeśli token istnieje i jest poprawny -> ustaw kontekst bezpieczeństwa
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                // Odczytaj username z tokena
                String username = jwtUtils.getUserNameFromJwtToken(jwt);

                // Pobierz pełne informacje o użytkowniku (authorities/role)
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // Utwórz obiekt Authentication zawierający user + role
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Ustaw uwierzytelnienie w kontekście Spring Security
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // Loguj, ale nie przerywaj łańcucha filtrów
            logger.error("Cannot set user authentication: {}", e.getMessage());
        }

        // Kontynuuj przetwarzanie żądania
        filterChain.doFilter(request, response);
    }

    /**
     * Wyodrębnia token JWT z nagłówka HTTP {@code Authorization}.
     *
     * <p>Nagłówek musi mieć format {@code Bearer <token>}.
     * Metoda usuwa prefix {@code "Bearer "} i zwraca sam token.</p>
     *
     * @param request przychodzące żądanie HTTP
     * @return ciąg znaków tokenu JWT lub {@code null}, jeśli nagłówek jest nieobecny
     *         bądź ma nieprawidłowy format
     */
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        // Sprawdź czy nagłówek istnieje i zaczyna się od "Bearer "
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            // Zwróć sam token (bez prefiksu "Bearer ")
            return headerAuth.substring(7);
        }

        // Brak poprawnego tokena w nagłówku
        return null;
    }
}
