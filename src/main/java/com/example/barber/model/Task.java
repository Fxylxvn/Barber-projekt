package com.example.barber.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "tasks")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private String status; // e.g., TODO, IN_PROGRESS, DONE

    @ManyToOne
    private User assignedTo;

    private boolean isPublic;
}
