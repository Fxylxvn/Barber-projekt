package com.example.barber_api.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Podstawowa encja transakcyjna reprezentująca Wizytę w systemie.
 * Zawiera w sobie relacje wielokrotne `@ManyToOne` do klientów, usług i barbarów.
 */
@Entity
@Table(name = "appointments")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Appointment {
    
    /**
     * Główny, losowo generowany klucz w bazie relacyjnej.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    /**
     * Mapowanie pracownika firmy, do którego przypisano tę rezerwację.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barber_id")
    private User barber;

    /**
     * Mapowanie klienta widniejącego na karcie wizyty (osoba, która się zarejestrowała).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private User client;

    /**
     * Ograniczenie rezerwacji do konkretnej operacji (np. strzyżenia).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private ServiceItem service;

    /**
     * Czas zajścia wizyty – w ujęciu lokalnym.
     */
    private LocalDateTime dateTime;
    
    /**
     * Krążące stany maszyny statusowej, np. PENDING, CONFIRMED, CANCELLED.
     */
    private String status; 

    public Appointment() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getBarber() { return barber; }
    public void setBarber(User barber) { this.barber = barber; }
    public User getClient() { return client; }
    public void setClient(User client) { this.client = client; }
    public ServiceItem getService() { return service; }
    public void setService(ServiceItem service) { this.service = service; }
    public LocalDateTime getDateTime() { return dateTime; }
    public void setDateTime(LocalDateTime dateTime) { this.dateTime = dateTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
