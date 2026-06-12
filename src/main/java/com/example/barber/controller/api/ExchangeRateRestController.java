package com.example.barber.controller.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/*
  Kontroler REST obsługujący pobieranie kursu wymiany EUR z NBP (Narodowy Bank Polski).
  Udostępnia kurs wymiany oraz dostarcza bezpieczną wartość rezerwową (fallback) w razie niedostępności usługi.
 */
@RestController
@RequestMapping("/api/exchange-rate")
public class ExchangeRateRestController {

    private static final String NBP_API_URL = "https://api.nbp.pl/api/exchangerates/rates/a/eur/?format=json";
    private final ObjectMapper objectMapper = new ObjectMapper();

    /*
      Pobiera aktualny kurs średni EUR z tabeli A NBP.
      
      @return JSON zawierający kurs (np. { "rate": 4.2851, "source": "NBP" })
     */
    @GetMapping("/eur")
    public ResponseEntity<?> getEurRate() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(4))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(NBP_API_URL))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode ratesNode = root.path("rates");
                if (ratesNode.isArray() && ratesNode.size() > 0) {
                    double midRate = ratesNode.get(0).path("mid").asDouble();
                    return ResponseEntity.ok(Map.of("rate", midRate, "source", "NBP"));
                }
            }
            return ResponseEntity.ok(Map.of("rate", 4.30, "source", "fallback", "warning", "Nieprawidłowy format odpowiedzi NBP"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("rate", 4.30, "source", "fallback", "error", e.getMessage()));
        }
    }
}
