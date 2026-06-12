package com.example.barber.controller.api;

import com.example.barber.model.Task;
import com.example.barber.model.User;
import com.example.barber.repo.TaskRepo;
import com.example.barber.repo.UserRepo;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
  Kontroler REST obsługujący operacje na zadaniach (tasks) przez API.
  <p>Dostępny pod ścieżką bazową {@code /api/tasks}. Wymagane role:
  ADMIN, MANAGER lub BARBER – patrz konfiguracja w
  {@link com.example.barber.config.SecurityConfig}.</p>

  <p>Obsługuje pełny CRUD dla zadań:
  <ul>
    <li><b>GET</b>    – lista zadań filtrowana według roli użytkownika.</li>
    <li><b>POST</b>   – tworzenie nowego zadania.</li>
    <li><b>PUT</b>    – aktualizacja istniejącego zadania.</li>
    <li><b>DELETE</b> – usuwanie zadania.</li>
  </ul>
  </p>
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskRestController {

    // Repozytorium zadań – operacje CRUD na encji {@link Task}.
    private final TaskRepo taskRepo;

    // Repozytorium użytkowników – pobieranie danych zalogowanego użytkownika. */
    private final UserRepo userRepo;

    /*
      Konstruktor wstrzykujący wymagane repozytoria przez Spring.

      @param taskRepo repozytorium zadań
      @param userRepo repozytorium użytkowników
     */
    public TaskRestController(TaskRepo taskRepo, UserRepo userRepo) {
        this.taskRepo = taskRepo;
        this.userRepo = userRepo;
    }

    /*
        Pobiera listę zadań dostosowaną do roli zalogowanego użytkownika.

      <p>Logika dostępu:
      <ul>
        <li>ADMIN / MANAGER – widzi wszystkie zadania w systemie.</li>
        <li>BARBER – widzi zadania przypisane do siebie oraz wszystkie zadania publiczne.</li>
      </ul>
      Duplikaty (zadanie przypisane i zarazem publiczne) są usuwane przez {@code distinct()}.</p>

      @param auth dane uwierzytelnienia zalogowanego użytkownika (wstrzykiwane przez Spring)
      @return lista zadań dopasowana do roli użytkownika
     */
    @GetMapping
    public List<Task> getTasks(Authentication auth) {
        User user = userRepo.findByUsername(auth.getName());
        if (user.getRole().equals("ADMIN") || user.getRole().equals("MANAGER")) {
            return taskRepo.findAll();
        }
        // Użytkownicy widzą zadania publiczne oraz przypisane do nich
        List<Task> tasks = taskRepo.findByAssignedTo(user);
        tasks.addAll(taskRepo.findByIsPublicTrue());
        return tasks.stream().distinct().toList();
    }

    /*
      Tworzy nowe zadanie w systemie.

      <p>Jeśli pole {@code assignedTo} nie jest podane w żądaniu, zadanie zostaje
      automatycznie przypisane do zalogowanego użytkownika.</p>

      @param task obiekt zadania deserializowany z ciała żądania JSON
      @param auth dane uwierzytelnienia zalogowanego użytkownika
      @return zapisany obiekt {@link Task} z nadanym ID
     */
    @PostMapping
    public Task createTask(@RequestBody Task task, Authentication auth) {
        if (task.getAssignedTo() == null) {
            User user = userRepo.findByUsername(auth.getName());
            task.setAssignedTo(user);
        }
        return taskRepo.save(task);
    }

    /*
      Aktualizuje istniejące zadanie o podanym identyfikatorze.
      <p>Nadpisuje pola: {@code title}, {@code description}, {@code status}
      oraz {@code isPublic} wartościami z ciała żądania.
      Pole {@code assignedTo} nie jest zmieniane podczas aktualizacji.</p>

      @param id          identyfikator zadania do aktualizacji
      @param taskDetails nowe dane zadania deserializowane z ciała żądania JSON
      @return {@code 200 OK} z zaktualizowanym obiektem {@link Task} lub
              {@code 404 Not Found} jeśli zadanie nie istnieje
     */
    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(@PathVariable("id") Long id, @RequestBody Task taskDetails) {
        return taskRepo.findById(id).map(task -> {
            task.setTitle(taskDetails.getTitle());
            task.setDescription(taskDetails.getDescription());
            task.setStatus(taskDetails.getStatus());
            task.setPublic(taskDetails.isPublic());
            return ResponseEntity.ok(taskRepo.save(task));
        }).orElse(ResponseEntity.notFound().build());
    }

    /*
      Usuwa zadanie o podanym identyfikatorze.

      <p>Nie sprawdza uprawnień – każdy z wymaganych ról (ADMIN, MANAGER, BARBER)
      może usunąć dowolne zadanie.</p>

      @param id identyfikator zadania do usunięcia
      @return {@code 204 No Content} po sukcesie lub
              {@code 404 Not Found} jeśli zadanie nie istnieje
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable("id") Long id) {
        if (taskRepo.existsById(id)) {
            taskRepo.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
