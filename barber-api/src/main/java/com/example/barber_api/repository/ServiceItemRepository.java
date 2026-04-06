package com.example.barber_api.repository;

import com.example.barber_api.entity.ServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Zapewnia pełen cykl zapytań CRUD dla zasobu usług w cenniku (ServiceItem).
 */
public interface ServiceItemRepository extends JpaRepository<ServiceItem, Long> {
}
