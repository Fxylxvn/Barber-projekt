package com.example.barber.controller;

import com.example.barber.model.Appointment;
import com.example.barber.model.User;
import com.example.barber.model.InspirationStyle;
import com.example.barber.repo.AppointmentRepo;
import com.example.barber.repo.UserRepo;
import com.example.barber.repo.InspirationStyleRepo;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/*
  Główny kontroler MVC aplikacji Barber obsługujący widoki webowe (Thymeleaf).

  <p>Odpowiada za renderowanie stron HTML dla wszystkich użytkowników systemu:
  klientów, barberów oraz gości. W odróżnieniu od kontrolerów REST ({@code /api/**})
  zwraca nazwy szablonów Thymeleaf, które są renderowane przez serwer do HTML.</p>

  <p>Obsługiwane obszary:
  <ul>
    <li><b>Publiczne</b> – strona logowania ({@code /login}), rejestracja ({@code /register}),
        strona informacyjna barbera ({@code /barber-info/{username}}).</li>
    <li><b>Panel klienta</b> – dashboard ({@code /client/dashboard}), rezerwacja wizyty
        ({@code /client/book}), interakcja ze stylami inspiracji.</li>
    <li><b>Panel barbera</b> – dashboard ({@code /barber/dashboard}), zarządzanie
        harmonogramem pracy, galerią zdjęć i stylami inspiracji.</li>
  </ul>
  </p>

  <p>Stałe {@code SERVICE_DURATION} i {@code SERVICE_PRICE} definiują czas trwania
  i cenę poszczególnych usług, używane przy tworzeniu rezerwacji.</p>
 */
@Controller
public class MainController {

    // Repozytorium użytkowników.
    private final UserRepo userRepo;

    // Repozytorium wizyt.
    private final AppointmentRepo appointmentRepo;

    // Koder BCrypt – używany przy rejestracji nowego konta.
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    // Repozytorium styli inspiracji.
    private final InspirationStyleRepo inspirationStyleRepo;

    /*
      Mapa czasu trwania poszczególnych usług w minutach.
      Klucz: nazwa usługi, wartość: czas trwania w minutach.
     */
    private static final Map<String, Integer> SERVICE_DURATION = Map.of(
        "Włosy", 45,
        "Broda", 30,
        "Klata", 15,
        "Włosy + Broda", 75,
        "Wszystko", 90
    );


    private static final Map<String, Integer> SERVICE_PRICE = Map.of(
        "Włosy", 50,
        "Broda", 35,
        "Klata", 20,
        "Włosy + Broda", 75,
        "Wszystko", 120
    );

    // Dopłata dla gości niezalogowanych: +25% do ceny bazowej.
    private static final double GUEST_PRICE_MULTIPLIER = 1.25;

    // Zniżka lojalnościowa dla zalogowanych: co 5. wizyta -30%.
    private static final double LOYALTY_DISCOUNT = 0.30;

    // Co ile wizyt przyznawana jest zniżka lojalnościowa.
    private static final int LOYALTY_INTERVAL = 5;

    /* Oblicza cenę dla gościa (cena bazowa + 25%). */
    private int calcGuestPrice(String serviceType) {
        int base = SERVICE_PRICE.getOrDefault(serviceType, 50);
        return (int) Math.ceil(base * GUEST_PRICE_MULTIPLIER);
    }

    /* Oblicza cenę z rabatem lojalnościowym (-30%). */
    private int calcLoyaltyPrice(String serviceType) {
        int base = SERVICE_PRICE.getOrDefault(serviceType, 50);
        return (int) Math.ceil(base * (1 - LOYALTY_DISCOUNT));
    }

    /*
     Sprawdza, czy kolejna wizyta klienta jest wizytą lojalnościową (co LOYALTY_INTERVAL wizyta).
     Zlicza dotychczasowe wizyty i jeśli (count + 1) % LOYALTY_INTERVAL == 0, stosuje zniżkę.
    */
    private boolean isNextVisitLoyalty(User client) {
        long count = appointmentRepo.countByClient(client);
        return (count + 1) % LOYALTY_INTERVAL == 0;
    }

    /* Zwraca ile wizyt pozostało do następnej zniżki lojalnościowej. */
    private long visitsUntilNextLoyalty(User client) {
        long count = appointmentRepo.countByClient(client);
        long remainder = LOYALTY_INTERVAL - ((count % LOYALTY_INTERVAL));
        return remainder == LOYALTY_INTERVAL ? 0 : remainder;
    }

    /*
     * Konstruktor wstrzykujący wymagane zależności przez Spring.
     *
     * @param userRepo             repozytorium użytkowników
     * @param appointmentRepo      repozytorium wizyt
     * @param passwordEncoder      koder haseł BCrypt
     * @param inspirationStyleRepo repozytorium styli inspiracji
     */
    public MainController(UserRepo userRepo, AppointmentRepo appointmentRepo,
                          org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
                          InspirationStyleRepo inspirationStyleRepo) {
        this.userRepo = userRepo;
        this.appointmentRepo = appointmentRepo;
        this.passwordEncoder = passwordEncoder;
        this.inspirationStyleRepo = inspirationStyleRepo;
    }

    /*
      Przekierowuje zalogowanego użytkownika na odpowiedni dashboard w zależności od roli.

      <p>BARBER → {@code /barber/dashboard}, KLIENT → {@code /client/dashboard},
      niezalogowany → {@code /login}.</p>

      @return redirect na odpowiedni adres URL
     */
    @GetMapping("/")
    public String index() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_BARBER"))) {
            return "redirect:/barber/dashboard";
        } else if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_KLIENT"))) {
            return "redirect:/client/dashboard";
        }
        return "redirect:/login";
    }

    /*
      Wyświetla stronę logowania.
      @return nazwa szablonu Thymeleaf: {@code login}
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /*
      Wyświetla formularz rejestracji nowego konta.
      @param model model Thymeleaf – dodawany pusty obiekt {@link User} dla formularza
      @return nazwa szablonu Thymeleaf: {@code register}
     */
    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    // ── GUEST BOOKING (bez konta) ─────────────────────────────────────────────

    /*
      Wyświetla formularz rezerwacji dla gości (niezalogowanych).
      Strona jest publicznie dostępna pod adresem {@code /book-guest}.
     */
    @GetMapping("/book-guest")
    public String bookGuestPage(Model model) {
        model.addAttribute("barbers", userRepo.findByRole("BARBER"));
        model.addAttribute("allAppointments", appointmentRepo.findAll());
        model.addAttribute("servicePrices", SERVICE_PRICE);
        return "guest_book";
    }

    /*
      Przetwarza formularz rezerwacji gościa (bez konta).

      <p>Waliduje termin (godziny pracy, dni robocze, kolizje) identycznie jak
      rezerwacja zalogowanego klienta. Cena jest wyższa o 25% względem ceny bazowej.
      Dane gościa (imię, email, telefon) są zapisywane bezpośrednio na wizycie.</p>

      @param barberId    ID wybranego barbera
      @param date        data i godzina wizyty (format ISO)
      @param serviceType rodzaj usługi
      @param notes       opcjonalne uwagi
      @param guestName   imię i nazwisko gościa
      @param guestEmail  e-mail gościa
      @param guestPhone  telefon gościa
      @return redirect na /book-guest z parametrem sukcesu lub błędu
     */
    @PostMapping("/book-guest")
    public String bookGuestVisit(@RequestParam("barberId") Long barberId,
                                 @RequestParam("date") String date,
                                 @RequestParam("serviceType") String serviceType,
                                 @RequestParam(value = "notes", required = false) String notes,
                                 @RequestParam("guestName") String guestName,
                                 @RequestParam("guestEmail") String guestEmail,
                                 @RequestParam("guestPhone") String guestPhone) {
        User barber = userRepo.findById(barberId).orElseThrow();
        LocalDateTime termin = LocalDateTime.parse(date, DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        // Walidacja: minuty co 15
        if (termin.getMinute() % 15 != 0) {
            return "redirect:/book-guest?error=NieprawidlowaMinuta";
        }
        // Walidacja: dzień roboczy
        int dayOfWeek = termin.getDayOfWeek().getValue();
        if (barber.getWorkDays() != null && !barber.getWorkDays().isEmpty()) {
            if (!barber.getWorkDays().contains(String.valueOf(dayOfWeek))) {
                return "redirect:/book-guest?error=DzienWolny";
            }
        }
        // Walidacja: godziny pracy
        int hour = termin.getHour();
        if (barber.getWorkStartHour() != null && barber.getWorkEndHour() != null) {
            if (hour < barber.getWorkStartHour() || hour >= barber.getWorkEndHour()) {
                return "redirect:/book-guest?error=PozaGodzinami";
            }
        }
        // Walidacja: kolizje
        int duration = SERVICE_DURATION.getOrDefault(serviceType, 60);
        LocalDateTime endTime = termin.plusMinutes(duration);
        for (Appointment existing : appointmentRepo.findByBarber(barber)) {
            LocalDateTime es = existing.getAppointmentDate();
            int ed = existing.getDurationMinutes() != null ? existing.getDurationMinutes() : 60;
            LocalDateTime ee = es.plusMinutes(ed);
            if (termin.isBefore(ee) && endTime.isAfter(es)) {
                return "redirect:/book-guest?error=Zajete";
            }
        }

        // Cena gościa = baza + 25%
        int price = calcGuestPrice(serviceType);

        Appointment appt = new Appointment();
        appt.setBarber(barber);
        appt.setAppointmentDate(termin);
        appt.setServiceType(serviceType);
        appt.setDurationMinutes(duration);
        appt.setDescription(serviceType);
        appt.setNotes(notes);
        appt.setGuest(true);
        appt.setGuestName(guestName);
        appt.setGuestEmail(guestEmail);
        appt.setGuestPhone(guestPhone);
        appt.setPriceCharged(price);
        appt.setDiscountApplied(false);
        appointmentRepo.save(appt);

        return "redirect:/book-guest?success";
    }

    /*
      Przetwarza formularz rejestracji i tworzy nowe konto klienta.

      <p>Hasło jest hashowane BCryptem przed zapisem do bazy.
      Nowe konto otrzymuje automatycznie rolę {@code "KLIENT"}.</p>

      @param user dane użytkownika z formularza rejestracji
      @return redirect na stronę logowania
     */
    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user) {
        user.setRole("KLIENT");
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepo.save(user);
        return "redirect:/login";
    }

    /*
      Wyświetla dashboard klienta z listą jego wizyt, dostępnych barberów i styli inspiracji.

      @param model model Thymeleaf wypełniany danymi dla widoku
      @return nazwa szablonu Thymeleaf: {@code client_dashboard}
     */
    @GetMapping("/client/dashboard")
    public String clientDashboard(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User client = userRepo.findByUsername(auth.getName());
        model.addAttribute("user", client);
        model.addAttribute("appointments", appointmentRepo.findByClient(client));
        model.addAttribute("barbers", userRepo.findByRole("BARBER"));
        model.addAttribute("allAppointments", appointmentRepo.findAll());
        model.addAttribute("inspirationStyles", inspirationStyleRepo.findAll());

        // System lojalnościowy
        long visitCount = appointmentRepo.countByClient(client);
        long untilNext = visitsUntilNextLoyalty(client);
        boolean nextIsLoyalty = isNextVisitLoyalty(client);
        model.addAttribute("visitCount", visitCount);
        model.addAttribute("untilNextLoyalty", untilNext);
        model.addAttribute("nextIsLoyalty", nextIsLoyalty);
        model.addAttribute("servicePrices", SERVICE_PRICE);

        return "client_dashboard";
    }

    /*
      Przetwarza formularz rezerwacji wizyty przez klienta.

      <p>Walidacja przed zapisem:
      <ol>
        <li>Minuta musi być wielokrotnością 15 (sloty co 15 minut).</li>
        <li>Wybrany dzień musi być dniem roboczym barbera.</li>
        <li>Godzina musi mieścić się w godzinach pracy barbera.</li>
        <li>Termin nie może kolidować z istniejącą wizytą barbera (wykrywanie nakładania się).</li>
      </ol>
      W przypadku błędu – redirect z parametrem błędu (np. {@code ?error=Zajete}).
      Po sukcesie – redirect z parametrem {@code ?success}.</p>

      @param barberId    ID wybranego barbera
      @param date        data i godzina wizyty w formacie ISO (np. {@code 2025-06-15T10:00})
      @param serviceType rodzaj usługi (np. "Włosy", "Broda")
      @param notes       opcjonalne uwagi klienta
      @param model       model Thymeleaf (nieużywany bezpośrednio, ale wymagany przez Spring)
      @return redirect na dashboard klienta z odpowiednim parametrem
     */
    @PostMapping("/client/book")
    public String bookVisit(@RequestParam("barberId") Long barberId,
                            @RequestParam("date") String date,
                            @RequestParam("serviceType") String serviceType,
                            @RequestParam(value = "notes", required = false) String notes,
                            Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User client = userRepo.findByUsername(auth.getName());
        User barber = userRepo.findById(barberId).orElseThrow();

        LocalDateTime termin = LocalDateTime.parse(date, DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        // Walidacja: minuty muszą być wielokrotnością 15
        int minute = termin.getMinute();
        if (minute % 15 != 0) {
            return "redirect:/client/dashboard?error=NieprawidlowaMinuta";
        }

        // Walidacja: wybrany dzień musi być dniem roboczym barbera
        int dayOfWeek = termin.getDayOfWeek().getValue();
        if (barber.getWorkDays() != null && !barber.getWorkDays().isEmpty()) {
            if (!barber.getWorkDays().contains(String.valueOf(dayOfWeek))) {
                return "redirect:/client/dashboard?error=DzienWolny";
            }
        }

        // Walidacja: godzina musi mieścić się w godzinach pracy barbera
        int appointmentHour = termin.getHour();
        if (barber.getWorkStartHour() != null && barber.getWorkEndHour() != null) {
            if (appointmentHour < barber.getWorkStartHour() || appointmentHour >= barber.getWorkEndHour()) {
                return "redirect:/client/dashboard?error=PozaGodzinami";
            }
        }

        // Walidacja: sprawdzenie kolizji z istniejącymi wizytami barbera
        int newServiceDuration = SERVICE_DURATION.getOrDefault(serviceType, 60);
        LocalDateTime newEndTime = termin.plusMinutes(newServiceDuration);

        List<Appointment> existingAppointments = appointmentRepo.findByBarber(barber);
        for (Appointment existing : existingAppointments) {
            LocalDateTime existingStart = existing.getAppointmentDate();
            int existingDuration = existing.getDurationMinutes() != null ? existing.getDurationMinutes() : 60;
            LocalDateTime existingEnd = existingStart.plusMinutes(existingDuration);

            // Wykryj nakładanie się terminów (overlap)
            if (termin.isBefore(existingEnd) && newEndTime.isAfter(existingStart)) {
                return "redirect:/client/dashboard?error=Zajete";
            }
        }

        // Sprawdź czy to wizyta lojalnościowa (co 5. wizyta -30%)
        boolean isLoyalty = isNextVisitLoyalty(client);
        int price = isLoyalty ? calcLoyaltyPrice(serviceType)
                              : SERVICE_PRICE.getOrDefault(serviceType, 50);

        // Utwórz i zapisz nową wizytę
        Appointment appointment = new Appointment();
        appointment.setClient(client);
        appointment.setBarber(barber);
        appointment.setAppointmentDate(termin);
        appointment.setServiceType(serviceType);
        appointment.setDurationMinutes(newServiceDuration);
        appointment.setDescription(serviceType);
        appointment.setNotes(notes);
        appointment.setPriceCharged(price);
        appointment.setDiscountApplied(isLoyalty);
        appointment.setGuest(false);
        appointmentRepo.save(appointment);

        return "redirect:/client/dashboard?success";
    }

    /*
      Wyświetla dashboard barbera z listą jego wizyt i styli inspiracji.

      @param model model Thymeleaf wypełniany danymi dla widoku
      @return nazwa szablonu Thymeleaf: {@code barber_dashboard}
     */
    @GetMapping("/barber/dashboard")
    public String barberDashboard(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User barber = userRepo.findByUsername(auth.getName());
        model.addAttribute("appointments", appointmentRepo.findByBarber(barber));
        model.addAttribute("allAppointments", appointmentRepo.findAll());
        model.addAttribute("barber", barber);
        model.addAttribute("inspirationStyles", inspirationStyleRepo.findAll());
        return "barber_dashboard";
    }

    /*
      Aktualizuje godziny i dni pracy zalogowanego barbera.

      <p>Jeśli lista dni jest pusta lub null, pole {@code workDays} barbera
      zostaje ustawione na {@code null} (brak wybranych dni roboczych).</p>

      @param workStartHour godzina rozpoczęcia pracy
      @param workEndHour   godzina zakończenia pracy
      @param workDays      lista wybranych dni roboczych (1=Pn, ..., 7=Nd); może być null
      @return redirect na dashboard barbera z parametrem {@code ?hoursUpdated}
     */
    @PostMapping("/barber/updateHours")
    public String updateWorkHours(@RequestParam("workStartHour") Integer workStartHour,
                                  @RequestParam("workEndHour") Integer workEndHour,
                                  @RequestParam(value = "workDays", required = false) List<String> workDays) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User barber = userRepo.findByUsername(auth.getName());
        barber.setWorkStartHour(workStartHour);
        barber.setWorkEndHour(workEndHour);
        if (workDays != null && !workDays.isEmpty()) {
            barber.setWorkDays(String.join(",", workDays));
        } else {
            barber.setWorkDays(null);
        }
        userRepo.save(barber);
        return "redirect:/barber/dashboard?hoursUpdated";
    }

    /*
     Usuwa wizytę o podanym ID (dostępne tylko dla barbera).

      @param id identyfikator wizyty do usunięcia
      @return redirect na dashboard barbera
     */
    @PostMapping("/barber/appointment/delete/{id}")
    public String deleteVisit(@PathVariable("id") Long id) {
        appointmentRepo.deleteById(id);
        return "redirect:/barber/dashboard";
    }

    /*
      Przełącza "lajk" klienta na styl inspiracji (dodaje lub usuwa).

      <p>Jeśli styl jest już na liście polubionych, zostaje usunięty;
      jeśli nie ma, zostaje dodany. Lista jest przechowywana jako
      string z wartościami oddzielonymi przecinkami w polu {@code likedStyles}.</p>

      @param styleId ID stylu inspiracji do przełączenia lajka
      @return tekst {@code "OK"} (odpowiedź AJAX)
     */
    @PostMapping("/client/like-style")
    @ResponseBody
    public String toggleLikeStyle(@RequestParam("styleId") String styleId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User client = userRepo.findByUsername(auth.getName());
        String liked = client.getLikedStyles();
        if (liked == null) {
            liked = "";
        }
        List<String> list = new java.util.ArrayList<>(java.util.Arrays.asList(liked.split(",")));
        list.removeIf(String::isEmpty);
        if (list.contains(styleId)) {
            list.remove(styleId);
        } else {
            list.add(styleId);
        }
        client.setLikedStyles(String.join(",", list));
        userRepo.save(client);
        return "OK";
    }

    /*
      Zapisuje wybrany przez klienta "wygrywający" styl inspiracji.

      <p>Wygrywający styl to styl, który klient chce odwzorować na wizycie.</p>

      @param winnerStyle ID lub nazwa wybranego stylu
      @return tekst {@code "OK"} (odpowiedź AJAX)
     */
    @PostMapping("/client/set-winner-style")
    @ResponseBody
    public String setWinnerStyle(@RequestParam("winnerStyle") String winnerStyle) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User client = userRepo.findByUsername(auth.getName());
        client.setWinnerStyle(winnerStyle);
        userRepo.save(client);
        return "OK";
    }

    /*
      Zapisuje preferencje klienta wprowadzone przez barbera.

      <p>Barber może notatować preferencje konkretnego klienta (np. ulubiony styl
      strzyżenia, długość boku itp.), które będą widoczne przy kolejnych wizytach.</p>

      @param clientId          ID klienta, którego preferencje są aktualizowane
      @param clientPreferences tekst opisujący preferencje klienta
      @return redirect na dashboard barbera z parametrem {@code ?preferencesSaved}
     */
    @PostMapping("/barber/client-preferences")
    public String saveClientPreferences(@RequestParam("clientId") Long clientId,
                                        @RequestParam("clientPreferences") String clientPreferences) {
        User client = userRepo.findById(clientId).orElseThrow();
        client.setClientPreferences(clientPreferences);
        userRepo.save(client);
        return "redirect:/barber/dashboard?preferencesSaved";
    }

    /*
      Zwraca szczegółowe informacje o kliencie wraz z historią jego wizyt (JSON).

      <p>Używane przez JavaScript na dashboardzie barbera do wyświetlenia
      informacji o kliencie w modalu bez przeładowywania strony.</p>

      @param id ID klienta
      @return mapa z danymi klienta i listą jego wizyt (serializowana jako JSON)
     */
    @GetMapping("/barber/client-details/{id}")
    @ResponseBody
    public Map<String, Object> getClientDetails(@PathVariable("id") Long id) {
        User client = userRepo.findById(id).orElseThrow();
        List<Appointment> appointments = appointmentRepo.findByClient(client);

        List<Map<String, Object>> apptsData = new java.util.ArrayList<>();
        for (Appointment a : appointments) {
            apptsData.add(Map.of(
                "date", a.getAppointmentDate().toString(),
                "barberName", a.getBarber() != null ? a.getBarber().getName() : "Nieznany",
                "serviceType", a.getServiceType(),
                "notes", a.getNotes() != null ? a.getNotes() : ""
            ));
        }

        return Map.of(
            "id", client.getId(),
            "name", client.getName(),
            "username", client.getUsername(),
            "clientPreferences", client.getClientPreferences() != null ? client.getClientPreferences() : "",
            "likedStyles", client.getLikedStyles() != null ? client.getLikedStyles() : "",
            "winnerStyle", client.getWinnerStyle() != null ? client.getWinnerStyle() : "",
            "appointments", apptsData
        );
    }

    /*
      Wyszukuje klientów po nazwie lub loginie (JSON).

      <p>Filtruje listę wszystkich klientów (case-insensitive) i zwraca
      uproszczone dane (ID, name, username). Używane przez wyszukiwarkę
      na dashboardzie barbera.</p>

      @param query fraza wyszukiwania (min. 1 znak)
      @return lista map z danymi pasujących klientów
     */
    @GetMapping("/barber/search-clients")
    @ResponseBody
    public List<Map<String, Object>> searchClients(@RequestParam("query") String query) {
        List<User> clients = userRepo.findByRole("KLIENT");
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (User c : clients) {
            if (c.getName().toLowerCase().contains(query.toLowerCase()) ||
                c.getUsername().toLowerCase().contains(query.toLowerCase())) {
                result.add(Map.of(
                    "id", c.getId(),
                    "name", c.getName(),
                    "username", c.getUsername()
                ));
            }
        }
        return result;
    }

    /*
      Pomocnicza klasa DTO przechowująca dane certyfikatu barbera.

      <p>Używana wyłącznie w widoku publicznej strony barbera ({@code /barber-info/{username}})
      do wyświetlenia informacji o certyfikacie zawodowym.</p>
     */
    public static class CertInfo {
        // URL do obrazka certyfikatu.
        private final String image;

        // Tytuł/nazwa certyfikatu.
        private final String title;

        // Opis certyfikatu i kwalifikacji, które potwierdza.
        private final String description;

        /*
          Tworzy nowy obiekt informacji o certyfikacie.

         * @param image       URL do zdjęcia/skanu certyfikatu
         * @param title       tytuł certyfikatu
         * @param description opis certyfikatu
         */
        public CertInfo(String image, String title, String description) {
            this.image = image;
            this.title = title;
            this.description = description;
        }


        public String getImage() { return image; }


        public String getTitle() { return title; }


        public String getDescription() { return description; }
    }

    /*
      Wyświetla publiczną stronę informacyjną wybranego barbera.

      <p>Strona jest dostępna publicznie (bez logowania) pod adresem
      {@code /barber-info/{username}}. Zawiera profil barbera, godziny pracy,
      galerię i informacje o certyfikacie.</p>

      @param username nazwa użytkownika barbera
      @param model    model Thymeleaf wypełniany danymi barbera
      @return nazwa szablonu Thymeleaf: {@code barber_info} lub redirect na {@code /login}
              jeśli barber nie istnieje
     */
    @GetMapping("/barber-info/{username}")
    public String barberInfo(@PathVariable("username") String username, Model model) {
        User barber = userRepo.findByUsername(username);
        if (barber == null || !"BARBER".equals(barber.getRole())) {
            return "redirect:/login";
        }
        model.addAttribute("barber", barber);

        // Konwertuj numery dni roboczych na polskie nazwy
        String workDaysFormatted = "Brak ustalonych dni pracy";
        if (barber.getWorkDays() != null && !barber.getWorkDays().isEmpty()) {
            String[] days = barber.getWorkDays().split(",");
            java.util.List<String> dayNames = new java.util.ArrayList<>();
            for (String d : days) {
                switch (d.trim()) {
                    case "1": dayNames.add("Poniedziałek"); break;
                    case "2": dayNames.add("Wtorek"); break;
                    case "3": dayNames.add("Środa"); break;
                    case "4": dayNames.add("Czwartek"); break;
                    case "5": dayNames.add("Piątek"); break;
                    case "6": dayNames.add("Sobota"); break;
                    case "7": dayNames.add("Niedziela"); break;
                }
            }
            workDaysFormatted = String.join(", ", dayNames);
        }
        model.addAttribute("workDaysFormatted", workDaysFormatted);

        // Przypisz odpowiedni certyfikat na podstawie nazwy użytkownika barbera
        CertInfo certInfo;
        if ("barber1".equals(username)) {
            certInfo = new CertInfo(
                "/img/cert1.png",
                "Certyfikat Mistrza Klasycznego Strzyżenia",
                "Ten prestiżowy certyfikat poświadcza najwyższe kwalifikacje Janusza Książkiewicza w zakresie klasycznej szkoły fryzjerstwa męskiego. Przyznany przez Międzynarodową Akademię Barberingu za mistrzowskie opanowanie tradycyjnego golenia brzytwą z rytuałem gorącego ręcznika oraz perfekcyjne cięcia typu Pompadour i Side Part."
            );
        } else if ("barber2".equals(username)) {
            certInfo = new CertInfo(
                "/img/cert2.png",
                "Certyfikat Nowoczesnych Technik Cieniowania",
                "Certyfikat przyznany Andrzejowi za wybitną precyzję w nowoczesnych technikach cieniowania (Low, Mid, High Fade) oraz geometrii męskiego zarostu. Szkolenie ukończone pod okiem czołowych światowych edukatorów, potwierdzające mistrzostwo w stylu Street i Modern Barbering."
            );
        } else if ("barber3".equals(username)) {
            certInfo = new CertInfo(
                "/img/cert3.png",
                "Certyfikat Stylistyki i Koloryzacji Męskiej",
                "Certyfikat poświadcza ekspercką wiedzę Krzysztofa w doborze fryzur do kształtu głowy i budowy twarzy (Visagisme) oraz zaawansowanej koloryzacji i maskowania siwizny. Przyznany za innowacyjne podejście do stylizacji i kreacji nowoczesnego wizerunku dżentelmena."
            );
        } else {
            certInfo = new CertInfo(
                "/img/cert1.png",
                "Certyfikat Profesjonalnego Barbera",
                "Certyfikat potwierdzający kwalifikacje zawodowe, dbałość o najwyższe standardy higieny oraz profesjonalną obsługę klienta w salonach klasy premium."
            );
        }
        model.addAttribute("cert", certInfo);

        return "barber_info";
    }

    /*
      Zapisuje wgrany plik na dysk i zwraca jego publiczny URL.

      <p>Plik jest zapisywany w katalogu {@code uploads/} w katalogu roboczym aplikacji.
      Nazwa pliku jest generowana losowo (UUID) z zachowaniem oryginalnego rozszerzenia,
      co chroni przed konfliktami nazw i atakami path traversal.</p>

      @param file wgrany plik multipart
      @return publiczny URL do pliku (np. {@code /uploads/abc123.jpg}) lub
              {@code null} jeśli plik jest pusty
      @throws IOException w przypadku błędu zapisu pliku na dysk
     */
    private String saveUploadedFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return null;
        }
        String uploadsDir = System.getProperty("user.dir") + "/uploads/";
        File directory = new File(uploadsDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String newFilename = UUID.randomUUID().toString() + extension;
        Path path = Paths.get(uploadsDir + newFilename);
        Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/" + newFilename;
    }

    /*
      Aktualizuje zdjęcie profilowe zalogowanego barbera.

      <p>Zapisuje przesłany plik na dysk i aktualizuje pole {@code photoUrl}
      w profilu barbera.</p>

      @param file wgrany plik ze zdjęciem profilowym
      @return redirect na dashboard barbera z parametrem sukcesu lub błędu
     */
    @PostMapping("/barber/profile/update-picture")
    public String updateProfilePicture(@RequestParam("profilePicture") MultipartFile file) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User barber = userRepo.findByUsername(auth.getName());
        try {
            String photoUrl = saveUploadedFile(file);
            if (photoUrl != null) {
                barber.setPhotoUrl(photoUrl);
                userRepo.save(barber);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "redirect:/barber/dashboard?error=UploadFailed";
        }
        return "redirect:/barber/dashboard?profilePictureUpdated";
    }

    /*
      Dodaje nowe zdjęcie do galerii zalogowanego barbera.

      <p>Zapisuje przesłany plik na dysk i dodaje jego URL do listy
      {@code galleryImages} barbera.</p>

      @param file wgrany plik ze zdjęciem do galerii
      @return redirect na dashboard barbera z parametrem sukcesu lub błędu
     */
    @PostMapping("/barber/gallery/add")
    public String addGalleryPhoto(@RequestParam("galleryPhoto") MultipartFile file) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User barber = userRepo.findByUsername(auth.getName());
        try {
            String imageUrl = saveUploadedFile(file);
            if (imageUrl != null) {
                barber.getGalleryImages().add(imageUrl);
                userRepo.save(barber);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "redirect:/barber/dashboard?error=UploadFailed";
        }
        return "redirect:/barber/dashboard?galleryPhotoAdded";
    }

    /*
      Usuwa zdjęcie z galerii zalogowanego barbera.

      <p>Usuwa URL z listy galerii barbera w bazie danych oraz usuwa
      fizyczny plik z dysku (jeśli jest w katalogu {@code /uploads/}).</p>

      @param imageUrl URL zdjęcia do usunięcia (np. {@code /uploads/abc123.jpg})
      @return redirect na dashboard barbera z parametrem sukcesu
     */
    @PostMapping("/barber/gallery/delete")
    public String deleteGalleryPhoto(@RequestParam("imageUrl") String imageUrl) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User barber = userRepo.findByUsername(auth.getName());
        if (barber.getGalleryImages().contains(imageUrl)) {
            barber.getGalleryImages().remove(imageUrl);
            userRepo.save(barber);
            try {
                String filename = imageUrl.replace("/uploads/", "");
                Path path = Paths.get(System.getProperty("user.dir") + "/uploads/" + filename);
                Files.deleteIfExists(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "redirect:/barber/dashboard?galleryPhotoDeleted";
    }

    /*
      Dodaje nowy styl inspiracji do systemu (przez barbera).

      <p>Barber może dodać nowe zdjęcie stylu fryzury lub zarostu,
      które będzie widoczne dla wszystkich klientów w sekcji inspiracji.</p>

      @param file     wgrany plik z obrazem stylu
      @param name     nazwa stylu (np. "Low Skin Fade")
      @param category kategoria stylu: {@code "hair"} lub {@code "beard"}
      @return redirect na dashboard barbera (sekcja inspiracji) z parametrem sukcesu lub błędu
     */
    @PostMapping("/barber/inspirations/add")
    public String addInspirationStyle(@RequestParam("inspirationFile") MultipartFile file,
                                      @RequestParam("name") String name,
                                      @RequestParam("category") String category) {
        try {
            String imageUrl = saveUploadedFile(file);
            if (imageUrl != null) {
                InspirationStyle style = new InspirationStyle();
                style.setName(name);
                style.setCategory(category);
                style.setUrl(imageUrl);
                inspirationStyleRepo.save(style);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "redirect:/barber/dashboard?error=UploadFailed";
        }
        return "redirect:/barber/dashboard?inspirationAdded#screen-inspirations";
    }

    /*
      Usuwa styl inspiracji o podanym ID.

      <p>Usuwa rekord z bazy danych oraz fizyczny plik z dysku
      (jeśli zdjęcie jest przechowywane lokalnie w katalogu {@code /uploads/}).</p>

      @param id ID stylu inspiracji do usunięcia
      @return redirect na dashboard barbera (sekcja inspiracji) z parametrem sukcesu
     */
    @PostMapping("/barber/inspirations/delete")
    public String deleteInspirationStyle(@RequestParam("id") Long id) {
        InspirationStyle style = inspirationStyleRepo.findById(id).orElse(null);
        if (style != null) {
            inspirationStyleRepo.delete(style);
            // Usuń plik z dysku jeśli to lokalny upload
            if (style.getUrl() != null && style.getUrl().startsWith("/uploads/")) {
                try {
                    String filename = style.getUrl().replace("/uploads/", "");
                    Path path = Paths.get(System.getProperty("user.dir") + "/uploads/" + filename);
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return "redirect:/barber/dashboard?inspirationDeleted#screen-inspirations";
    }
}
