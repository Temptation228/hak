package com.borisey.personal_finance.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "banks")
public class Bank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String bik;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Bank() {
        this.createdAt = LocalDateTime.now();
    }

    public Bank(String title, String bik) {
        this.title = title;
        this.bik = bik;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBik() {
        return bik;
    }

    public void setBik(String bik) {
        this.bik = bik;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}