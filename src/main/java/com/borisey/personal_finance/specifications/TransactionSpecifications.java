package com.borisey.personal_finance.specifications;

import com.borisey.personal_finance.models.Transaction;
import com.borisey.personal_finance.models.TransactionStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionSpecifications {

    public static Specification<Transaction> belongsToUser(Long userId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Transaction> hasSenderBank(Long bankId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("senderBank").get("id"), bankId);
    }

    public static Specification<Transaction> hasRecipientBank(Long bankId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("recipientBank").get("id"), bankId);
    }

    public static Specification<Transaction> notDeleted() {
        return (root, query, criteriaBuilder) -> {
            Join<Transaction, TransactionStatus> statusJoin = root.join("status", JoinType.INNER);
            return criteriaBuilder.notEqual(statusJoin.get("code"), "DELETED");
        };
    }

    public static Specification<Transaction> dateIsBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.between(root.get("operationDateTime"), startDate, endDate);
    }

    public static Specification<Transaction> hasStatus(Long statusId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("status").get("id"), statusId);
    }

    public static Specification<Transaction> hasInn(String inn) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("recipientInn"), inn);
    }

    public static Specification<Transaction> amountIsBetween(BigDecimal minAmount, BigDecimal maxAmount) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.between(root.get("amount"), minAmount, maxAmount);
    }

    public static Specification<Transaction> hasTransactionType(Long typeId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("transactionType").get("id"), typeId);
    }

    public static Specification<Transaction> hasCategory(Long categoryId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("category").get("id"), categoryId);
    }
}
