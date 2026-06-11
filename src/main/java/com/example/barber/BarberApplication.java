package com.example.barber;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Główna klasa startowa aplikacji Barber.
 *
 * <p>Uruchamia cały kontekst Spring Boot – skanuje pakiety, konfiguruje
 * auto-konfigurację, ładuje beany i startuje wbudowany serwer Tomcat.</p>
 */
@SpringBootApplication
public class BarberApplication {

    /**
     * Punkt wejścia do aplikacji.
     *
     * @param args argumenty wiersza poleceń przekazywane przez JVM
     */
    public static void main(String[] args) {
        SpringApplication.run(BarberApplication.class, args);
    }

}
