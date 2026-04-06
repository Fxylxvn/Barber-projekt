package com.example.barber_api;

import com.example.barber_api.entity.ServiceItem;
import com.example.barber_api.entity.User;
import com.example.barber_api.repository.ServiceItemRepository;
import com.example.barber_api.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Automatyczny Bean (Komponent Springa), który służy nam w ramach makiety projektu.
 * Wywołuje się tuż po podniesieniu szkieletu aplikacji włączając na sucho 
 * dane konfiguracyjne - niezbędne aby aplikacja nie była "pusta" i gotowa na demonstrację.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ServiceItemRepository serviceRepository;

    public DataInitializer(UserRepository userRepository, ServiceItemRepository serviceRepository) {
        this.userRepository = userRepository;
        this.serviceRepository = serviceRepository;
    }

    /**
     * Nadpisana wirtualna maszyna - ta komenda uruchamia skrypt H2 ładujący obiekty.
     */
    @Override
    public void run(String... args) throws Exception {
        userRepository.save(new User("admin@example.com", "password", "Admin", "Adminowy", "0000", "ADMIN"));
        userRepository.save(new User("barber1@example.com", "password", "Jan", "Kowalski", "1111", "BARBER"));
        userRepository.save(new User("barber2@example.com", "password", "Adam", "Nowak", "2222", "BARBER"));
        userRepository.save(new User("klient@example.com", "password", "Michał", "Testowy", "3333", "CLIENT"));

        serviceRepository.save(new ServiceItem("Strzyżenie męskie", 60.0, 40));
        serviceRepository.save(new ServiceItem("Trymowanie brody", 40.0, 20));
    }
}
