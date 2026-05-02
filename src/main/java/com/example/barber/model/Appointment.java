package com.example.barber.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime appointmentDate;

    @ManyToOne
    private User client;

    @ManyToOne
    private User barber;

    private String description;

    private String serviceType;

    private Integer durationMinutes;
}


