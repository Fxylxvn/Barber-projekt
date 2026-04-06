package com.example.barber_api.entity;

import jakarta.persistence.*;

/**
 * Encja reprezentująca konkretną ofertę widniejącą w cenniku (katalogu).
 * To za jej pomocą decydujesz, jak bardzo powiększa się rachunek po wyborze usługi.
 */
@Entity
@Table(name = "services")
public class ServiceItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nazwa usługi w systemie, wyświetlana frontendzie UI.
     */
    private String name;
    
    /**
     * Bazowa cena – wykorzystywana np. do wstępnej wyceny koszyka u Barbera.
     */
    private Double price;
    
    /**
     * Okres trwania pozwalający wyznaczyć bloki godzinne w kalendarzu u pracownika.
     */
    private Integer durationInMin;

    public ServiceItem() {}

    public ServiceItem(String name, Double price, Integer durationInMin) {
        this.name = name;
        this.price = price;
        this.durationInMin = durationInMin;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public Integer getDurationInMin() { return durationInMin; }
    public void setDurationInMin(Integer durationInMin) { this.durationInMin = durationInMin; }
}
