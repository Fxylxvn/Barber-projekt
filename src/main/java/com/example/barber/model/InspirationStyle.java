package com.example.barber.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/*
  Encja reprezentująca styl inspiracji (fryzura lub zarost) wyświetlany klientom.

  <p>Przechowywana w tabeli {@code inspiration_styles}.
  Barber może dodawać nowe style (wraz z miniaturką), a klienci mogą je
  "lajkować" i wybierać jako swój preferowany wygląd podczas rezerwacji.</p>

  <p>Lombok {@code @Data} generuje gettery, settery, {@code toString},
  {@code equals} i {@code hashCode}.</p>
 */
@Entity
@Data
@Table(name = "inspiration_styles")
public class InspirationStyle {

    // Unikalny identyfikator stylu, generowany automatycznie przez bazę danych.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nazwa stylu, np. "Low Skin Fade", "Classic Full Beard".
    private String name;

    /*
      Kategoria stylu – określa, do jakiego rodzaju usługi styl się odnosi.
      Dopuszczalne wartości: {@code "hair"} (fryzura) lub {@code "beard"} (zarost).
     */
    private String category;

    /*
      URL do zdjęcia prezentującego dany styl.
      Może być zewnętrznym linkiem (np. Unsplash) lub lokalną ścieżką {@code /uploads/...}.
     */
    private String url;
}
