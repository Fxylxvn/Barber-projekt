package com.example.barber_api.repository;

import com.example.barber_api.entity.GalleryItem;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Baza obrazów portofolio barberów. Obiekt JpaRepository dostarcza 
 * standardowych funkcji: save, findById, findAll, delete.
 */
public interface GalleryItemRepository extends JpaRepository<GalleryItem, Long> {
}
