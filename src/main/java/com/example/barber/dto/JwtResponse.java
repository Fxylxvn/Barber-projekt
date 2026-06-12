package com.example.barber.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/*
  DTO (Data Transfer Object) zwracany klientowi po pomyślnym logowaniu przez API.

  <p>Zawiera trzy pola:
  <ul>
    <li>{@code token}    – wygenerowany token JWT, który klient musi dołączyć
        do kolejnych żądań w nagłówku {@code Authorization: Bearer <token>}.</li>
    <li>{@code username} – nazwa zalogowanego użytkownika (login/e-mail).</li>
    <li>{@code role}     – rola użytkownika (np. {@code KLIENT} lub {@code BARBER}),
        przydatna do decyzji po stronie frontendu o tym, który widok wyświetlić.</li>
  </ul>
  Lombok {@code @Data} generuje gettery, settery, {@code toString}, {@code equals} i {@code hashCode}.
  Lombok {@code @AllArgsConstructor} generuje konstruktor przyjmujący wszystkie pola.
  </p>
 */
@Data
@AllArgsConstructor
public class JwtResponse {
    // Wygenerowany token JWT do autoryzacji kolejnych zapytań.
    private String token;

    // Nazwa (login) zalogowanego użytkownika.
    private String username;

    // Rola użytkownika w systemie (np. KLIENT, BARBER).
    private String role;
}
