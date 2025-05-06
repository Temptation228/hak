package com.borisey.personal_finance.models;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "transaction_statuses")
public class TransactionStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String title;

    @OneToMany(mappedBy = "status")
    private List<Transaction> transactions;

    public TransactionStatus() {}

    public TransactionStatus(String code, String title) {
        this.code = code;
        this.title = title;
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
}
