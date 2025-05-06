package com.borisey.personal_finance.repo;

import com.borisey.personal_finance.models.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TransactionStatusRepository extends JpaRepository<TransactionStatus, Long> {
    Optional<TransactionStatus> findByCode(String code);
}
