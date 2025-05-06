package com.borisey.personal_finance.repo;

import com.borisey.personal_finance.models.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TransactionTypeRepository extends JpaRepository<TransactionType, Long> {
    Optional<TransactionType> findByCode(String code);
}
