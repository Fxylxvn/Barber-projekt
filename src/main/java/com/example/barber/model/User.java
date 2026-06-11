package com.example.barber.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import lombok.Data;
import java.util.List;
import java.util.ArrayList;

@Entity
@Data
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String password;
    private String role;
    private String name;
    private String email;
    private String firstName;
    private String lastName;

    private Integer workStartHour;
    private Integer workEndHour;

    private String workDays;

    // Barber portfolio fields
    private String photoUrl;
    private String title;
    private Double rating;
    private String bio;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_gallery_images", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "image_url")
    private List<String> galleryImages = new ArrayList<>();

    // Client preferences and styles
    private String clientPreferences;
    private String likedStyles;
    private String winnerStyle;
}


