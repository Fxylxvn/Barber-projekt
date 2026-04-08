package com.example.barber_api.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;

/**
 * Podstawowa encja służąca jako przechowalnia "prac portofolio" barbera.
 * Symuluje zapis pliku np. na chmurę S3.
 */
@Entity
@Table(name = "gallery_items")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class GalleryItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    // Przechowywanie statycznego identyfikatora ścieżki
    private String imageUrl;
    
    // Potencjalny podpis autorski lub nazwa fryzury
    private String description;

    // Połączenie z pracownikiem, który pochwalił się zdjęciem z pracy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") 
    private User uploader;

    public GalleryItem() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public User getUploader() { return uploader; }
    public void setUploader(User uploader) { this.uploader = uploader; }
}
