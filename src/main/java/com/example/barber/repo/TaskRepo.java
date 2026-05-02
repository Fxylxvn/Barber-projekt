package com.example.barber.repo;

import com.example.barber.model.Task;
import com.example.barber.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepo extends JpaRepository<Task, Long> {
    List<Task> findByAssignedTo(User user);
    List<Task> findByIsPublicTrue();
}
