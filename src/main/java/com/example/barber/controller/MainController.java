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

@Controller
public class MainController {

    private final UserRepo userRepo;
    private final AppointmentRepo appointmentRepo;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final InspirationStyleRepo inspirationStyleRepo;

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

    public MainController(UserRepo userRepo, AppointmentRepo appointmentRepo, org.springframework.security.crypto.password.PasswordEncoder passwordEncoder, InspirationStyleRepo inspirationStyleRepo) {
        this.userRepo = userRepo;
        this.appointmentRepo = appointmentRepo;
        this.passwordEncoder = passwordEncoder;
        this.inspirationStyleRepo = inspirationStyleRepo;
    }

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

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user) {
        user.setRole("KLIENT");
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepo.save(user);
        return "redirect:/login";
    }

    @GetMapping("/client/dashboard")
    public String clientDashboard(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User client = userRepo.findByUsername(auth.getName());
        model.addAttribute("user", client);
        model.addAttribute("appointments", appointmentRepo.findByClient(client));
        model.addAttribute("barbers", userRepo.findByRole("BARBER"));
        model.addAttribute("allAppointments", appointmentRepo.findAll());
        model.addAttribute("inspirationStyles", inspirationStyleRepo.findAll());
        return "client_dashboard";
    }

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

        int minute = termin.getMinute();
        if (minute % 15 != 0) {
            return "redirect:/client/dashboard?error=NieprawidlowaMinuta";
        }

        int dayOfWeek = termin.getDayOfWeek().getValue();
        if (barber.getWorkDays() != null && !barber.getWorkDays().isEmpty()) {
            if (!barber.getWorkDays().contains(String.valueOf(dayOfWeek))) {
                return "redirect:/client/dashboard?error=DzienWolny";
            }
        }

        int appointmentHour = termin.getHour();
        if (barber.getWorkStartHour() != null && barber.getWorkEndHour() != null) {
            if (appointmentHour < barber.getWorkStartHour() || appointmentHour >= barber.getWorkEndHour()) {
                return "redirect:/client/dashboard?error=PozaGodzinami";
            }
        }

        int newServiceDuration = SERVICE_DURATION.getOrDefault(serviceType, 60);
        LocalDateTime newEndTime = termin.plusMinutes(newServiceDuration);

        List<Appointment> existingAppointments = appointmentRepo.findByBarber(barber);
        for (Appointment existing : existingAppointments) {
            LocalDateTime existingStart = existing.getAppointmentDate();
            int existingDuration = existing.getDurationMinutes() != null ? existing.getDurationMinutes() : 60;
            LocalDateTime existingEnd = existingStart.plusMinutes(existingDuration);

            if (termin.isBefore(existingEnd) && newEndTime.isAfter(existingStart)) {
                return "redirect:/client/dashboard?error=Zajete";
            }
        }

        Appointment appointment = new Appointment();
        appointment.setClient(client);
        appointment.setBarber(barber);
        appointment.setAppointmentDate(termin);
        appointment.setServiceType(serviceType);
        appointment.setDurationMinutes(newServiceDuration);
        appointment.setDescription(serviceType);
        appointment.setNotes(notes);
        appointmentRepo.save(appointment);

        return "redirect:/client/dashboard?success";
    }

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

    @PostMapping("/barber/updateHours")
    public String updateWorkHours(@RequestParam("workStartHour") Integer workStartHour, @RequestParam("workEndHour") Integer workEndHour,
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

    @PostMapping("/barber/appointment/delete/{id}")
    public String deleteVisit(@PathVariable("id") Long id) {
        appointmentRepo.deleteById(id);
        return "redirect:/barber/dashboard";
    }

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

    @PostMapping("/client/set-winner-style")
    @ResponseBody
    public String setWinnerStyle(@RequestParam("winnerStyle") String winnerStyle) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User client = userRepo.findByUsername(auth.getName());
        client.setWinnerStyle(winnerStyle);
        userRepo.save(client);
        return "OK";
    }

    @PostMapping("/barber/client-preferences")
    public String saveClientPreferences(@RequestParam("clientId") Long clientId, @RequestParam("clientPreferences") String clientPreferences) {
        User client = userRepo.findById(clientId).orElseThrow();
        client.setClientPreferences(clientPreferences);
        userRepo.save(client);
        return "redirect:/barber/dashboard?preferencesSaved";
    }

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

    public static class CertInfo {
        private final String image;
        private final String title;
        private final String description;

        public CertInfo(String image, String title, String description) {
            this.image = image;
            this.title = title;
            this.description = description;
        }

        public String getImage() { return image; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
    }

    @GetMapping("/barber-info/{username}")
    public String barberInfo(@PathVariable("username") String username, Model model) {
        User barber = userRepo.findByUsername(username);
        if (barber == null || !"BARBER".equals(barber.getRole())) {
            return "redirect:/login";
        }
        model.addAttribute("barber", barber);

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

    @PostMapping("/barber/inspirations/delete")
    public String deleteInspirationStyle(@RequestParam("id") Long id) {
        InspirationStyle style = inspirationStyleRepo.findById(id).orElse(null);
        if (style != null) {
            inspirationStyleRepo.delete(style);
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

