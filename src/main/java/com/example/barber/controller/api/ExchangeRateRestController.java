package com.example.barber.controller.api;

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
  Wykorzystuje ręczne parsowanie odpowiedzi JSON, aby uniknąć problemów z zależnościami kompilacji.
 */
@RestController
@RequestMapping("/api/exchange-rate")
public class ExchangeRateRestController {

    private static final String NBP_API_URL = "https://api.nbp.pl/api/exchangerates/rates/a/eur/?format=json";

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
                double midRate = extractRateFromJson(response.body());
                return ResponseEntity.ok(Map.of("rate", midRate, "source", "NBP"));
            }
            return ResponseEntity.ok(Map.of("rate", 4.30, "source", "fallback", "warning", "Status odpowiedzi NBP: " + response.statusCode()));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("rate", 4.30, "source", "fallback", "error", e.getMessage()));
        }
    }

    /*
      Ręczna ekstrakcja kursu "mid" z odpowiedzi JSON z NBP.
      Przykładowy format: ... "mid":4.2543 ...
     */
    private double extractRateFromJson(String json) throws Exception {
        String searchKey = "\"mid\":";
        int start = json.indexOf(searchKey);
        if (start == -1) {
            throw new Exception("Brak pola 'mid' w odpowiedzi JSON");
        }
        start += searchKey.length();
        
        // Znajdź koniec liczby (np. do przecinka, nawiasu klamrowego lub kwadratowego)
        int end = json.length();
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == ',' || c == '}' || c == ']') {
                end = i;
                break;
            }
        }
        
        String rateStr = json.substring(start, end).trim();
        return Double.parseDouble(rateStr);
    }
}
