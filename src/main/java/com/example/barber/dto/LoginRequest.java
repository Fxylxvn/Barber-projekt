package com.example.barber.dto;

import lombok.Data;

/*
  DTO (Data Transfer Object) reprezentujący dane logowania przesyłane przez klienta.

  <p>Używany jako ciało żądania ({@code @RequestBody}) w endpoincie
  {@code POST /api/auth/login}. Klient przesyła JSON z polami
  {@code username} i {@code password}, które Spring automatycznie
  deserializuje do tej klasy.</p>

  <p>Lombok {@code @Data} generuje gettery, settery, {@code toString},
  {@code equals} i {@code hashCode}.</p>
 */
@Data
public class LoginRequest {

    // Nazwa użytkownika (login lub adres e-mail).
    private String username;

    // Hasło użytkownika w postaci jawnego tekstu (przed weryfikacją po stronie serwera).
    private String password;
}
