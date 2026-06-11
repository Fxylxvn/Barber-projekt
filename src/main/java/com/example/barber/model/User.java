package com.example.barber.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import lombok.Data;
import java.util.List;
import java.util.ArrayList;

/**
 * Encja reprezentująca użytkownika systemu – może to być klient lub barber.
 *
 * <p>Przechowywana w tabeli {@code users}. Rola użytkownika ({@code role}) decyduje
 * o tym, do jakich widoków i funkcji ma dostęp:
 * <ul>
 *   <li>{@code KLIENT}  – może rezerwować wizyty i przeglądać inspiracje.</li>
 *   <li>{@code BARBER}  – zarządza wizytami, galerią zdjęć i stylami inspiracji.</li>
 * </ul>
 * </p>
 *
 * <p>Klasa pełni też funkcję encji portfela barbera, przechowując zdjęcia galerii,
 * tytuł zawodowy, ocenę i bio.</p>
 *
 * <p>Lombok {@code @Data} generuje gettery, settery, {@code toString},
 * {@code equals} i {@code hashCode}.</p>
 */
@Entity
@Data
@Table(name = "users")
public class User {

    /** Unikalny identyfikator użytkownika, generowany automatycznie przez bazę danych. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Login użytkownika (zazwyczaj adres e-mail). Musi być unikalny. */
    private String username;

    /** Hasło użytkownika zahaszowane algorytmem BCrypt. */
    private String password;

    /**
     * Rola użytkownika w systemie.
     * Możliwe wartości: {@code KLIENT}, {@code BARBER}.
     * Spring Security poprzedza rolę prefiksem {@code ROLE_} automatycznie.
     */
    private String role;

    /** Pełna nazwa wyświetlana użytkownika (imię i nazwisko). */
    private String name;

    /** Adres e-mail użytkownika. */
    private String email;

    /** Imię użytkownika (opcjonalne, uzupełniające pole {@code name}). */
    private String firstName;

    /** Nazwisko użytkownika (opcjonalne, uzupełniające pole {@code name}). */
    private String lastName;

    // ── Pola harmonogramu pracy barbera ──────────────────────────────────────

    /** Godzina rozpoczęcia pracy barbera (np. {@code 9} oznacza 09:00). */
    private Integer workStartHour;

    /** Godzina zakończenia pracy barbera (np. {@code 18} oznacza 18:00). */
    private Integer workEndHour;

    /**
     * Dni robocze barbera jako string z wartościami oddzielonymi przecinkami.
     * Wartości: 1=Poniedziałek, 2=Wtorek, ..., 7=Niedziela.
     * Przykład: {@code "1,2,3,4,5"} – od poniedziałku do piątku.
     */
    private String workDays;

    // ── Pola portfolio barbera ────────────────────────────────────────────────

    /** URL do zdjęcia profilowego barbera. */
    private String photoUrl;

    /** Tytuł zawodowy barbera, np. "Senior Master Barber", "Junior Barber". */
    private String title;

    /** Ocena barbera w skali 1–5 (średnia z recenzji). */
    private Double rating;

    /** Krótki opis/biogram barbera wyświetlany na jego stronie profilowej. */
    private String bio;

    /**
     * Kolekcja URL-i zdjęć w galerii barbera.
     * Przechowywana w oddzielnej tabeli {@code user_gallery_images}
     * (relacja element-kolekcja, ładowana natychmiast – EAGER).
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_gallery_images", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "image_url")
    private List<String> galleryImages = new ArrayList<>();

    // ── Pola preferencji klienta ──────────────────────────────────────────────

    /**
     * Tekstowe preferencje klienta zapisane przez barbera,
     * np. "lubi krótkie boki, długi czub".
     */
    private String clientPreferences;

    /**
     * Lista ID stylów inspiracji, które klient "lajkował",
     * przechowywana jako string z wartościami oddzielonymi przecinkami.
     * Przykład: {@code "3,7,12"}.
     */
    private String likedStyles;

    /**
     * ID wybranego przez klienta "wygrywającego" stylu inspiracji
     * (styl, który klient chce odwzorować podczas wizyty).
     */
    private String winnerStyle;
}
