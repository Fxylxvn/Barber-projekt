package com.example.barber.controller.api;

import com.example.barber.model.Task;
import com.example.barber.model.User;
import com.example.barber.repo.TaskRepo;
import com.example.barber.repo.UserRepo;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskRestController {

    private final TaskRepo taskRepo;
    private final UserRepo userRepo;

    public TaskRestController(TaskRepo taskRepo, UserRepo userRepo) {
        this.taskRepo = taskRepo;
        this.userRepo = userRepo;
    }

    @GetMapping
    public List<Task> getTasks(Authentication auth) {
        User user = userRepo.findByUsername(auth.getName());
        if (user.getRole().equals("ADMIN") || user.getRole().equals("MANAGER")) {
            return taskRepo.findAll();
        }
        // Users can see public tasks or those assigned to them
        List<Task> tasks = taskRepo.findByAssignedTo(user);
        tasks.addAll(taskRepo.findByIsPublicTrue());
        return tasks.stream().distinct().toList();
    }

    @PostMapping
    public Task createTask(@RequestBody Task task, Authentication auth) {
        if (task.getAssignedTo() == null) {
            User user = userRepo.findByUsername(auth.getName());
            task.setAssignedTo(user);
        }
        return taskRepo.save(task);
    }

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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable("id") Long id) {
        if (taskRepo.existsById(id)) {
            taskRepo.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
