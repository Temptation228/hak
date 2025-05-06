package com.borisey.personal_finance.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "person_type_id", nullable = false)
    private PersonType personType;

    @JsonFormat(pattern = "dd.MM.yyyy [HH:mm:ss]", shape = JsonFormat.Shape.STRING)
    @Column(nullable = false)
    private LocalDateTime operationDateTime;

    @ManyToOne
    @JoinColumn(name = "transaction_type_id", nullable = false)
    private TransactionType transactionType;

    @Lob
    @Column(length = 1000)
    private String comment;

    @Digits(integer = 10, fraction = 5)
    @Column(nullable = false, precision = 15, scale = 5)
    private BigDecimal amount;

    @ManyToOne
    @JoinColumn(name = "status_id", nullable = false)
    private TransactionStatus status;

    @ManyToOne
    @JoinColumn(name = "sender_bank_id")
    private Bank senderBank;

    @Column(name = "source_account_number")
    private String sourceAccountNumber;

    @ManyToOne
    @JoinColumn(name = "recipient_bank_id")
    private Bank recipientBank;

    @Pattern(regexp = "\\d{10,12}", message = "ИНН должен состоять из 10-12 цифр")
    private String recipientInn;

    @Column(name = "recipient_account_number")
    private String recipientAccountNumber;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @Pattern(regexp = "^(\\+7|8)\\d{10}$", message = "Телефон должен начинаться с +7 или 8 и содержать 11 цифр")
    private String recipientPhone;

    @JsonFormat(pattern = "dd.MM.yyyy HH:mm:ss")
    @Column(nullable = false, updatable = false)
    private LocalDateTime created;

    @JsonFormat(pattern = "dd.MM.yyyy HH:mm:ss")
    private LocalDateTime updated;

    public Transaction() {
        this.created = LocalDateTime.now();
        this.updated = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public PersonType getPersonType() {
        return personType;
    }

    public void setPersonType(PersonType personType) {
        this.personType = personType;
    }

    public LocalDateTime getOperationDateTime() {
        return operationDateTime;
    }

    public void setOperationDateTime(LocalDateTime operationDateTime) {
        this.operationDateTime = operationDateTime;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public Bank getSenderBank() {
        return senderBank;
    }

    public void setSenderBank(Bank senderBank) {
        this.senderBank = senderBank;
    }

    public String getSourceAccountNumber() {
        return sourceAccountNumber;
    }

    public void setSourceAccountNumber(String sourceAccountNumber) {
        this.sourceAccountNumber = sourceAccountNumber;
    }

    public Bank getRecipientBank() {
        return recipientBank;
    }

    public void setRecipientBank(Bank recipientBank) {
        this.recipientBank = recipientBank;
    }

    public String getRecipientInn() {
        return recipientInn;
    }

    public void setRecipientInn(String recipientInn) {
        this.recipientInn = recipientInn;
    }

    public String getRecipientAccountNumber() {
        return recipientAccountNumber;
    }

    public void setRecipientAccountNumber(String recipientAccountNumber) {
        this.recipientAccountNumber = recipientAccountNumber;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public void setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public void setUpdated(LocalDateTime updated) {
        this.updated = updated;
    }

    // Методы для статусов транзакций
    public boolean isEditable() {
        return status.getCode().equals("NEW");
    }

    public boolean isDeletable() {
        String statusCode = status.getCode();
        return !statusCode.equals("CONFIRMED") && !statusCode.equals("PROCESSING") &&
                !statusCode.equals("CANCELLED") && !statusCode.equals("COMPLETED") &&
                !statusCode.equals("RETURNED");
    }
}
