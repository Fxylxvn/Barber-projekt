package com.example.barber_api.controller;

import com.example.barber_api.entity.GalleryItem;
import com.example.barber_api.repository.GalleryItemRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

/**
 * Zarządza publiczną galerią zdjęć.
 */
@RestController
@RequestMapping("/api/gallery")
@Tag(name = "Galeria", description = "Zarządzanie zdjęciami")
public class GalleryController {

    private final GalleryItemRepository galleryRepository;

    public GalleryController(GalleryItemRepository galleryRepository) {
        this.galleryRepository = galleryRepository;
    }

    /**
     * Zwraca obiekty z tabeli galerii.
     */
    @Operation(summary = "Pobranie wszystkich zdjęć")
    @GetMapping
    public ResponseEntity<List<GalleryItem>> getGallery() {
        return ResponseEntity.ok(galleryRepository.findAll());
    }

    /**
     * Zapisuje do bazy przesłane zdjęcie z opisem.
     */
    @Operation(summary = "Wstawienie nowego zdjęcia")
    @PostMapping
    public ResponseEntity<GalleryItem> uploadGalleryImage(@RequestParam("file") MultipartFile file, @RequestParam("description") String description) {
        GalleryItem item = new GalleryItem();
        item.setImageUrl("https://example.com/images/" + file.getOriginalFilename());
        item.setDescription(description);
        
        GalleryItem saved = galleryRepository.save(item);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Usuwa element po ID.
     */
    @Operation(summary = "Usunięcie zdjęcia")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGalleryImage(@PathVariable Long id) {
        galleryRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
