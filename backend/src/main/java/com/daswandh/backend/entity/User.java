package com.daswandh.backend.entity;

import com.daswandh.backend.enums.Role;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users") // "user" is a reserved word in PostgreSQL, so the table is "users"
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Postgres generates the id
    private Long id;

    private String email;
    private String passwordHash;
    private String fullName;

    @Enumerated(EnumType.STRING) // stores "CREATOR", not 1
    private Role role;

    private boolean enabled = true;

    @Column(updatable = false)
    private Instant createdAt;

    protected User() {} // required by JPA, hidden from the rest of the code

    public User(String email, String passwordHash, String fullName, Role role) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
    }

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public Role getRole() { return role; }
    public boolean isEnabled() { return enabled; }
    public Instant getCreatedAt() { return createdAt; }
}