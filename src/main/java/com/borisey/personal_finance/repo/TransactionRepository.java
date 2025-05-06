package com.borisey.personal_finance.repo;

import com.borisey.personal_finance.models.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    // Находит транзакции по пользователю
    List<Transaction> findByUserId(Long userId);

    // Поиск с пагинацией
    Page<Transaction> findByUserId(Long userId, Pageable pageable);

    // Поиск по пользователю и ID транзакции
    Transaction findByIdAndUserId(Long id, Long userId);

    // Поиск по диапазону дат
    List<Transaction> findByUserIdAndOperationDateTimeBetween(
            Long userId, LocalDateTime start, LocalDateTime end);

    // Поиск по банку отправителя
    List<Transaction> findByUserIdAndSenderBankId(Long userId, Long senderBankId);

    // Поиск по банку получателя
    List<Transaction> findByUserIdAndRecipientBankId(Long userId, Long recipientBankId);

    // Поиск по статусу
    List<Transaction> findByUserIdAndStatusId(Long userId, Long statusId);

    // Поиск по ИНН
    List<Transaction> findByUserIdAndRecipientInnContaining(Long userId, String inn);

    // Поиск по диапазону сумм
    List<Transaction> findByUserIdAndAmountBetween(
            Long userId, BigDecimal minAmount, BigDecimal maxAmount);

    // Поиск по типу транзакции
    List<Transaction> findByUserIdAndTransactionTypeId(Long userId, Long transactionTypeId);

    // Поиск по категории
    List<Transaction> findByUserIdAndCategoryId(Long userId, Long categoryId);

    // Статистические запросы для дашбордов

    // Количество транзакций по периоду (неделя/месяц/квартал/год)
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.user.id = :userId " +
            "AND t.operationDateTime BETWEEN :startDate AND :endDate")
    Long countTransactionsByPeriod(@Param("userId") Long userId,
                                   @Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);

    // Суммы по типам транзакций (дебет/кредит)
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId " +
            "AND t.transactionType.code = :typeCode " +
            "AND t.operationDateTime BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByTransactionType(@Param("userId") Long userId,
                                          @Param("typeCode") String typeCode,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    // Количество транзакций по статусам
    @Query("SELECT t.status.code, COUNT(t) FROM Transaction t " +
            "WHERE t.user.id = :userId GROUP BY t.status.code")
    List<Object[]> countTransactionsByStatus(@Param("userId") Long userId);

    // Статистика по банкам
    @Query("SELECT b.title, COUNT(t) FROM Transaction t " +
            "JOIN t.senderBank b WHERE t.user.id = :userId GROUP BY b.title")
    List<Object[]> countTransactionsBySenderBank(@Param("userId") Long userId);

    @Query("SELECT b.title, COUNT(t) FROM Transaction t " +
            "JOIN t.recipientBank b WHERE t.user.id = :userId GROUP BY b.title")
    List<Object[]> countTransactionsByRecipientBank(@Param("userId") Long userId);

    // Статистика по категориям
    @Query("SELECT c.title, SUM(t.amount) FROM Transaction t " +
            "JOIN t.category c WHERE t.user.id = :userId " +
            "AND t.transactionType.code = :typeCode GROUP BY c.title")
    List<Object[]> sumAmountByCategory(@Param("userId") Long userId,
                                       @Param("typeCode") String typeCode);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.user.id = :userId " +
            "AND t.operationDateTime BETWEEN :startDate AND :endDate " +
            "AND t.status.code <> 'DELETED'")
    Long countNonDeletedTransactionsByPeriod(@Param("userId") Long userId,
                                             @Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId " +
            "AND t.transactionType.code IN :typeCodes " +
            "AND t.operationDateTime BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByTransactionTypes(@Param("userId") Long userId,
                                           @Param("typeCodes") List<String> typeCodes,
                                           @Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);


}
