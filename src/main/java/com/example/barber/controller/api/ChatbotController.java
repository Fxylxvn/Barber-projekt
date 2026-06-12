package com.example.barber.controller.api;

import com.example.barber.model.Appointment;
import com.example.barber.model.InspirationStyle;
import com.example.barber.model.User;
import com.example.barber.repo.AppointmentRepo;
import com.example.barber.repo.InspirationStyleRepo;
import com.example.barber.repo.UserRepo;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/*
  Kontroler REST obsługujący chatbota AI dla klientów salonu.

  <p>Dostępny pod ścieżką {@code POST /api/chatbot/message}. Wymagana rola:
  KLIENT. Komunikuje się z lokalnym modelem Ollama (llama3.2:3b)
  pod adresem {@code http://localhost:11434/api/chat}.</p>

  <p>Przed każdym zapytaniem do LLM buduje dynamiczny system prompt
  zawierający aktualne dane z bazy: barberów, ich harmonogramy, dostępne
  usługi, wizyty klienta oraz style inspiracji. Dzięki temu chatbot
  odpowiada wyłącznie na pytania dotyczące salonu.</p>
 */
@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    private static final String OLLAMA_URL = "http://localhost:11434/api/chat";
    private static final String MODEL_NAME = "llama3.2:3b";
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final UserRepo userRepo;
    private final AppointmentRepo appointmentRepo;
    private final InspirationStyleRepo inspirationStyleRepo;

    public ChatbotController(UserRepo userRepo,
                             AppointmentRepo appointmentRepo,
                             InspirationStyleRepo inspirationStyleRepo) {
        this.userRepo = userRepo;
        this.appointmentRepo = appointmentRepo;
        this.inspirationStyleRepo = inspirationStyleRepo;
    }

    /*
      Przetwarza wiadomość klienta i zwraca odpowiedź chatbota.

      <p>Buduje kontekst z bazy danych (barberzy, wizyty, style)
      i przesyła go jako system prompt do Ollamy. Model odpowiada
      wyłącznie w zakresie tematycznym salonu.</p>

      @param body   mapa z kluczem {@code "message"} zawierającym pytanie klienta
      @param auth   dane uwierzytelnienia zalogowanego klienta
      @return JSON {@code { "response": "..." }} lub błąd
     */
    @PostMapping("/message")
    public ResponseEntity<?> chat(@RequestBody Map<String, String> body,
                                  Authentication auth) {
        String userMessage = body.get("message");
        if (userMessage == null || userMessage.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Wiadomość nie może być pusta."));
        }

        // Pobierz zalogowanego klienta
        User client = userRepo.findByUsername(auth.getName());
        if (client == null) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Nie znaleziono klienta."));
        }

        // Zbuduj kontekst z bazy danych
        String systemPrompt = buildSystemPrompt(client);

        // Wywołaj Ollama API
        try {
            String ollamaResponse = callOllama(systemPrompt, userMessage);
            return ResponseEntity.ok(Map.of("response", ollamaResponse));
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(503)
                    .body(Map.of("error",
                            "Nie mogę połączyć się z asystentem. Upewnij się, że Ollama jest uruchomiona na porcie 11434."));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Błąd wewnętrzny chatbota: " + e.getMessage()));
        }
    }

    /*
      Buduje system prompt zawierający dane kontekstowe salonu i klienta.
     */
    private String buildSystemPrompt(User client) {
        StringBuilder sb = new StringBuilder();

        sb.append("Jesteś pomocnym asystentem salonu fryzjerskiego GENTLEMAN'S CLUB Barber Club. ");
        sb.append("Odpowiadasz WYŁĄCZNIE na pytania związane z tym salonem: barberów, wizyt, usług, ");
        sb.append("harmonogramów pracy i stylów inspiracji. ");
        sb.append("Jeśli pytanie nie dotyczy salonu, uprzejmie poinformuj, że możesz pomóc tylko w sprawach salonu. ");
        sb.append("Odpowiadaj zawsze po polsku, zwięźle i pomocnie.\n\n");

        // === DOSTĘPNE USŁUGI ===
        sb.append("=== DOSTĘPNE USŁUGI I CENY ===\n");
        sb.append("- Strzyżenie włosów: 45 min, 50 zł\n");
        sb.append("- Trymowanie brody: 30 min, 35 zł\n");
        sb.append("- Combo: Włosy + Broda: 75 min, 75 zł\n");
        sb.append("- Golenie klatki piersiowej: 15 min, 20 zł\n");
        sb.append("- Królewski pakiet (Wszystko): 90 min, 120 zł\n\n");

        // === BARBERZY ===
        List<User> barbers = userRepo.findByRole("BARBER");
        sb.append("=== BARBERZY SALONU ===\n");
        for (User barber : barbers) {
            sb.append("- ").append(barber.getName());
            if (barber.getTitle() != null) {
                sb.append(" (").append(barber.getTitle()).append(")");
            }
            if (barber.getRating() != null) {
                sb.append(", ocena: ").append(barber.getRating()).append("/5.0");
            }
            if (barber.getWorkStartHour() != null && barber.getWorkEndHour() != null) {
                sb.append(", godziny pracy: ")
                  .append(barber.getWorkStartHour()).append(":00 - ")
                  .append(barber.getWorkEndHour()).append(":00");
            }
            if (barber.getWorkDays() != null && !barber.getWorkDays().isEmpty()) {
                sb.append(", dni pracy: ").append(formatWorkDays(barber.getWorkDays()));
            }
            if (barber.getBio() != null) {
                sb.append("\n  Opis: ").append(barber.getBio());
            }
            sb.append("\n");
        }
        sb.append("\n");

        // === WIZYTY KLIENTA ===
        List<Appointment> appointments = appointmentRepo.findByClient(client);
        sb.append("=== WIZYTY KLIENTA: ").append(client.getName()).append(" ===\n");
        if (appointments.isEmpty()) {
            sb.append("Klient nie ma żadnych zarezerwowanych wizyt.\n");
        } else {
            for (Appointment appt : appointments) {
                sb.append("- Data: ").append(appt.getAppointmentDate().format(DATE_FMT));
                sb.append(", Usługa: ").append(appt.getServiceType());
                sb.append(", Czas: ").append(appt.getDurationMinutes()).append(" min");
                if (appt.getBarber() != null) {
                    sb.append(", Barber: ").append(appt.getBarber().getName());
                }
                if (appt.getNotes() != null && !appt.getNotes().isBlank()) {
                    sb.append(", Uwagi: ").append(appt.getNotes());
                }
                sb.append("\n");
            }
        }
        sb.append("\n");

        // === STYLE INSPIRACJI ===
        List<InspirationStyle> styles = inspirationStyleRepo.findAll();
        sb.append("=== DOSTĘPNE STYLE INSPIRACJI ===\n");
        sb.append("Fryzury: ");
        styles.stream().filter(s -> "hair".equals(s.getCategory()))
              .forEach(s -> sb.append(s.getName()).append(", "));
        sb.append("\nZarosty i brody: ");
        styles.stream().filter(s -> "beard".equals(s.getCategory()))
              .forEach(s -> sb.append(s.getName()).append(", "));
        sb.append("\n\n");

        // === PREFERENCJE KLIENTA ===
        if (client.getWinnerStyle() != null && !client.getWinnerStyle().isBlank()) {
            sb.append("=== WYBRANY STYL KLIENTA ===\n");
            sb.append("Klient wybrał styl: ").append(client.getWinnerStyle()).append("\n\n");
        }
        if (client.getClientPreferences() != null && !client.getClientPreferences().isBlank()) {
            sb.append("=== PREFERENCJE KLIENTA ===\n");
            sb.append(client.getClientPreferences()).append("\n\n");
        }

        sb.append("Pamiętaj: odpowiadaj TYLKO po polsku i TYLKO w zakresie tematycznym salonu.");

        return sb.toString();
    }

    /*
      Konwertuje ciąg numerów dni (np. "1,2,3,4,5") na polskie nazwy dni tygodnia.
     */
    private String formatWorkDays(String workDays) {
        String[] parts = workDays.split(",");
        StringBuilder days = new StringBuilder();
        for (String d : parts) {
            switch (d.trim()) {
                case "1" -> days.append("Pon, ");
                case "2" -> days.append("Wt, ");
                case "3" -> days.append("Śr, ");
                case "4" -> days.append("Czw, ");
                case "5" -> days.append("Pt, ");
                case "6" -> days.append("Sob, ");
                case "7" -> days.append("Nd, ");
            }
        }
        String result = days.toString();
        if (result.endsWith(", ")) {
            result = result.substring(0, result.length() - 2);
        }
        return result;
    }

    /*
      Wywołuje Ollama REST API i zwraca odpowiedź tekstową modelu.
      Używa java.net.http.HttpClient (dostępne od Java 11, bez dodatkowych bibliotek).
      Ręcznie buduje JSON request i parsuje response bez Jacksona.

      @param systemPrompt kontekst systemowy z danymi salonu
      @param userMessage  wiadomość wpisana przez klienta
      @return odpowiedź modelu jako string
      @throws IOException          gdy nie można połączyć się z Ollamą
      @throws InterruptedException gdy połączenie zostało przerwane
     */
    private String callOllama(String systemPrompt, String userMessage)
            throws IOException, InterruptedException {

        // Bezpieczne escapowanie tekstu do JSON (podstawowe znaki specjalne)
        String escapedSystem = escapeJson(systemPrompt);
        String escapedUser = escapeJson(userMessage);

        // Ręczne budowanie JSON body dla Ollama /api/chat
        String requestJson = String.format(
            "{\"model\":\"%s\",\"stream\":false,\"messages\":[" +
            "{\"role\":\"system\",\"content\":\"%s\"}," +
            "{\"role\":\"user\",\"content\":\"%s\"}" +
            "]}",
            MODEL_NAME, escapedSystem, escapedUser
        );

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OLLAMA_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Ollama zwróciła błąd HTTP: " + response.statusCode()
                    + " - " + response.body());
        }

        // Prosta ekstrakcja pola "content" z JSON response Ollamy
        // Format: {"message":{"role":"assistant","content":"..."},...}
        return extractContentFromOllamaResponse(response.body());
    }

    /*
      Ekstrahuje wartość pola "content" z odpowiedzi JSON Ollamy bez biblioteki Jackson.
      Szuka wzorca "content":"..." w odpowiedzi.
     */
    private String extractContentFromOllamaResponse(String json) throws IOException {
        // Szukaj "content":" w JSONie
        String searchKey = "\"content\":\"";
        int contentStart = json.indexOf(searchKey);
        if (contentStart == -1) {
            throw new IOException("Brak pola 'content' w odpowiedzi Ollamy: " + json.substring(0, Math.min(200, json.length())));
        }
        contentStart += searchKey.length();

        // Znajdź koniec wartości - szukaj zamykającego cudzysłowu (uwzględniając escapowanie)
        StringBuilder result = new StringBuilder();
        int i = contentStart;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                switch (next) {
                    case '"' -> result.append('"');
                    case '\\' -> result.append('\\');
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    default -> result.append(next);
                }
                i += 2;
            } else if (c == '"') {
                break; // koniec stringa JSON
            } else {
                result.append(c);
                i++;
            }
        }
        return result.toString().isBlank() ? "Nie mam na to odpowiedzi." : result.toString();
    }

    /*
      Escapuje string do formatu JSON (obsługa znaków specjalnych).
     */
    private String escapeJson(String text) {
        if (text == null) return "";
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
