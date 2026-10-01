package com.mahmoud.tazkara.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity 
@Table (name = "users")
public class User {
    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;
    @Column (nullable = false, unique = true)
    private String email;
    @Column(nullable = false)
    private String passwordHash;
    @Enumerated (EnumType.STRING)
    @Column(nullable = false)
    private Plan plan;

    public User() {
    }

    public User(Long id, String email, String passwordHash, Plan plan) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.plan = plan;
    }

    public Long id() {
        return id;
    }

    public String email() {
        return email;
    }
    
    public String passwordHash() {
        return passwordHash;
    }

    public Plan plan() {
        return plan;
    }
}
