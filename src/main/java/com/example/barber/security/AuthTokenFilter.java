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

@Component
public class AuthTokenFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);
    /*
      Pomocnicze zależności wstrzykiwane przez Springa:
     `jwtUtils`  : operacje na tokenach (walidacja, odczyt username)
     userDetailsService` : pobiera pełne dane użytkownika (w tym role)
     */
    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    /*
      Konstruktor wstrzykuje potrzebne serwisy.
     */
    public AuthTokenFilter(JwtUtils jwtUtils, UserDetailsService userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

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
