package com.example.barber_api.controller;

import com.example.barber_api.entity.ServiceItem;
import com.example.barber_api.repository.ServiceItemRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Kontroler odpowiedzialny za udostępnianie cennika i oferty usług.
 */
@RestController
@RequestMapping("/api/services")
@Tag(name = "Usługi", description = "Zarządzanie usługami fryzjerskimi")
public class ServiceController {

    private final ServiceItemRepository serviceRepository;

    public ServiceController(ServiceItemRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    /**
     * Fetchuje kompletny cennik prosto z bazy w formie oryginalnej encji.
     */
    @Operation(summary = "Pobranie cennika usług")
    @GetMapping
    public ResponseEntity<List<ServiceItem>> getServices() {
        return ResponseEntity.ok(serviceRepository.findAll());
    }

    /**
     * Zapisuje cennik operując na JSONowej reprezentacji tabeli z bazy.
     */
    @Operation(summary = "Dodanie nowej usługi")
    @PostMapping
    public ResponseEntity<ServiceItem> createService(@RequestBody ServiceItem item) {
        ServiceItem saved = serviceRepository.save(item);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Podmienia cenę i nazwę usługi podając cały nowy obiekt.
     */
    @Operation(summary = "Skorygowanie danych usługi")
    @PutMapping("/{id}")
    public ResponseEntity<ServiceItem> updateService(@PathVariable Long id, @RequestBody ServiceItem newData) {
        return serviceRepository.findById(id).map(s -> {
            s.setName(newData.getName());
            s.setPrice(newData.getPrice());
            s.setDurationInMin(newData.getDurationInMin());
            ServiceItem saved = serviceRepository.save(s);
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Usuwa usługę.
     */
    @Operation(summary = "Trwałe usunięcie z cennika")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteService(@PathVariable Long id) {
        serviceRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
