package com.example.barber.repo;

import com.example.barber.model.InspirationStyle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InspirationStyleRepo extends JpaRepository<InspirationStyle, Long> {
}
