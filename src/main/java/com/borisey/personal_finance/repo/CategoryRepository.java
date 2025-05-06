package com.borisey.personal_finance.repo;

import com.borisey.personal_finance.models.Category;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByUserId(Long userId, Sort sort);

    List<Category> findByUserIdAndTransactionTypeId(Long userId, Long transactionTypeId, Sort sort);

    Category findByIdAndUserId(Long id, Long userId);

    @Query("SELECT c, SUM(t.amount) as totalAmount FROM Category c " +
                  "LEFT JOIN c.transactions t " +
                  "WHERE c.user.id = :userId AND c.transactionType.id = :typeId " +
                  "AND (t IS NULL OR (t.operationDateTime BETWEEN :startDate AND :endDate)) " +
                  "GROUP BY c.id")
    List<Object[]> findCategoriesWithTotalAmount(@Param("userId") Long userId,
                                                 @Param("typeId") Long typeId,
                                                 @Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate,
                                                 Sort sort);

    @Query("SELECT c FROM Category c WHERE c.user.id = :userId AND LOWER(c.title) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Category> searchByTitle(@Param("userId") Long userId, @Param("searchTerm") String searchTerm);
}