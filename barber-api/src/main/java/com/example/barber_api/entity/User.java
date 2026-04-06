package com.example.barber_api.entity;

import jakarta.persistence.*;

/**
 * Najszersza encja personalna – dziedziczy w sobie bazowo zachowania wszystkich
 * form ról: Od klienta przez barbera, aż po globalnego administratora. 
 */
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String phone;
    
    /**
     * W przyszłości potencjalnie zamienialne na tabelę ManyToMany w Spring Security,
     * u nas określa pole, tj. "CLIENT", "BARBER", "ADMIN" dla prostej autoryzacji.
     */
    private String role; 

    // Konstruktory obowiązkowe dla mechaniki JPA Hibernate
    public User() {}

    public User(String email, String password, String firstName, String lastName, String phone, String role) {
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.role = role;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
