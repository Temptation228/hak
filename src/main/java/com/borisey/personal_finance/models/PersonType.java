package com.borisey.personal_finance.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "person_types")
public class PersonType {

    public static final String INDIVIDUAL = "INDIVIDUAL";
    public static final String LEGAL = "LEGAL";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String title;

    @OneToMany(mappedBy = "personType")
    private List<Transaction> transactions;

    @JsonFormat(pattern = "dd.MM.yyyy HH:mm:ss")
    @Column(nullable = false, updatable = false)
    private LocalDateTime created;

    public PersonType() {
        this.created = LocalDateTime.now();
    }

    public PersonType(String code, String title) {
        this.code = code;
        this.title = title;
        this.created = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }
}
