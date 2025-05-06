package com.borisey.personal_finance.repo;

import com.borisey.personal_finance.models.Bank;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BankRepository extends JpaRepository<Bank, Long> {
    List<Bank> findAll(Sort sort);

    @Query("SELECT b FROM Bank b WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Bank> searchByTitle(@Param("searchTerm") String searchTerm);

    Bank findByBik(String bik);
}
