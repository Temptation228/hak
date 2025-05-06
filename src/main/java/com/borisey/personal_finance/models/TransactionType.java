package com.borisey.personal_finance.models;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "transaction_types")
public class TransactionType {

    public static final String INCOME = "INCOME";
    public static final String EXPENSE = "EXPENSE";
    public static final String TRANSFER = "TRANSFER";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String title;

    @OneToMany(mappedBy = "transactionType")
    private List<Transaction> transactions;

    public TransactionType() {}

    public TransactionType(String code, String title) {
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
