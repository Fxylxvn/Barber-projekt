package com.example.barber.model;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Encja reprezentująca zadanie (task) przypisane do użytkownika w salonie.
 *
 * <p>Przechowywana w tabeli {@code tasks}. Zadania mogą być prywatne
 * (widoczne tylko dla przypisanego użytkownika) lub publiczne – dostępne
 * dla wszystkich zalogowanych użytkowników z rolą BARBER lub wyżej.</p>
 *
 * <p>Typowe statusy zadania: {@code TODO}, {@code IN_PROGRESS}, {@code DONE}.</p>
 *
 * <p>Lombok {@code @Data} generuje gettery, settery, {@code toString},
 * {@code equals} i {@code hashCode}.</p>
 */
@Entity
@Data
@Table(name = "tasks")
public class Task {

    /** Unikalny identyfikator zadania, generowany automatycznie przez bazę danych. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tytuł zadania – krótki, opisowy nagłówek. */
    private String title;

    /** Szczegółowy opis zadania. */
    private String description;

    /**
     * Aktualny status zadania.
     * Przykładowe wartości: {@code TODO}, {@code IN_PROGRESS}, {@code DONE}.
     */
    private String status;

    /**
     * Użytkownik, do którego przypisano zadanie.
     * Relacja wiele-do-jednego: wielu zadań może być przypisanych do jednego użytkownika.
     */
    @ManyToOne
    private User assignedTo;

    /**
     * Flaga widoczności zadania.
     * Gdy {@code true} – zadanie jest widoczne dla wszystkich uprawnionych użytkowników;
     * gdy {@code false} – tylko dla przypisanego użytkownika.
     */
    private boolean isPublic;
}
