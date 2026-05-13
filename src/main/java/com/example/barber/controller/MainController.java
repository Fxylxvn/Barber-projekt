package com.example.barber.controller;

import com.example.barber.model.Appointment;
import com.example.barber.model.User;
import com.example.barber.repo.AppointmentRepo;
import com.example.barber.repo.UserRepo;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Controller
public class MainController {

    private final UserRepo userRepo;
    private final AppointmentRepo appointmentRepo;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

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

    public MainController(UserRepo userRepo, AppointmentRepo appointmentRepo, org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.appointmentRepo = appointmentRepo;
        this.passwordEncoder = passwordEncoder;
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
        return "client_dashboard";
    }

    @PostMapping("/client/book")
    public String bookVisit(@RequestParam("barberId") Long barberId, @RequestParam("date") String date, @RequestParam("serviceType") String serviceType, Model model) {
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
}

