package com.borisey.personal_finance.controllers;

import com.borisey.personal_finance.models.*;
import com.borisey.personal_finance.repo.*;
import com.borisey.personal_finance.specifications.TransactionSpecifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/transactions")
@PreAuthorize("hasRole('USER')")
public class TransactionController {

    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);
    private final TransactionRepository transactionRepository;
    private final TransactionStatusRepository statusRepository;
    private final UserRepository userRepository;

    @Autowired
    public TransactionController(
            TransactionRepository transactionRepository,
            TransactionStatusRepository statusRepository,
            UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.statusRepository = statusRepository;
        this.userRepository = userRepository;
        logger.info("Инициализирован контроллер транзакций");
    }

    @GetMapping
    public ResponseEntity<?> getAllTransactions(
            Authentication authentication,
            @RequestParam(required = false) Long senderBankId,
            @RequestParam(required = false) Long recipientBankId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd.MM.yyyy") LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd.MM.yyyy") LocalDateTime endDate,
            @RequestParam(required = false) Long statusId,
            @RequestParam(required = false) String inn,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) Long transactionTypeId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "operationDateTime") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        try {
            User user = userRepository.findByUsername(authentication.getName());
            logger.info("Пользователь '{}' запрашивает список транзакций (страница {}, размер {})",
                    user.getUsername(), page, size);

            if (senderBankId != null) logger.debug("Фильтр по банку отправителя: {}", senderBankId);
            if (recipientBankId != null) logger.debug("Фильтр по банку получателя: {}", recipientBankId);
            if (startDate != null) logger.debug("Фильтр по начальной дате: {}", startDate);
            if (endDate != null) logger.debug("Фильтр по конечной дате: {}", endDate);
            if (statusId != null) logger.debug("Фильтр по статусу: {}", statusId);
            if (inn != null) logger.debug("Фильтр по ИНН: {}", inn);
            if (minAmount != null) logger.debug("Фильтр по минимальной сумме: {}", minAmount);
            if (maxAmount != null) logger.debug("Фильтр по максимальной сумме: {}", maxAmount);
            if (transactionTypeId != null) logger.debug("Фильтр по типу транзакции: {}", transactionTypeId);
            if (categoryId != null) logger.debug("Фильтр по категории: {}", categoryId);

            Specification<Transaction> spec = Specification.where(
                            TransactionSpecifications.belongsToUser(user.getId()))
                    .and(TransactionSpecifications.notDeleted());

            if (senderBankId != null) {
                spec = spec.and(TransactionSpecifications.hasSenderBank(senderBankId));
            }

            if (recipientBankId != null) {
                spec = spec.and(TransactionSpecifications.hasRecipientBank(recipientBankId));
            }

            if (startDate != null && endDate != null) {
                spec = spec.and(TransactionSpecifications.dateIsBetween(startDate, endDate));
            }

            if (statusId != null) {
                spec = spec.and(TransactionSpecifications.hasStatus(statusId));
            }

            if (inn != null) {
                spec = spec.and(TransactionSpecifications.hasInn(inn));
            }

            if (minAmount != null && maxAmount != null) {
                spec = spec.and(TransactionSpecifications.amountIsBetween(minAmount, maxAmount));
            }

            if (transactionTypeId != null) {
                spec = spec.and(TransactionSpecifications.hasTransactionType(transactionTypeId));
            }

            if (categoryId != null) {
                spec = spec.and(TransactionSpecifications.hasCategory(categoryId));
            }

            Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

            Page<Transaction> transactions = transactionRepository.findAll(spec, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("transactions", transactions.getContent());
            response.put("currentPage", transactions.getNumber());
            response.put("totalItems", transactions.getTotalElements());
            response.put("totalPages", transactions.getTotalPages());

            logger.info("Для пользователя '{}' найдено {} транзакций (всего {}) на странице {} из {}",
                    user.getUsername(), transactions.getContent().size(), transactions.getTotalElements(),
                    page + 1, transactions.getTotalPages());

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Ошибка при получении списка транзакций: {}", e.getMessage(), e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTransactionById(
            Authentication authentication,
            @PathVariable Long id) {

        try {
            User user = userRepository.findByUsername(authentication.getName());
            logger.info("Пользователь '{}' запрашивает транзакцию с ID: {}", user.getUsername(), id);

            Transaction transaction = transactionRepository.findByIdAndUserId(id, user.getId());

            if (transaction == null || transaction.getStatus().getCode().equals("DELETED")) {
                logger.warn("Транзакция с ID: {} не найдена для пользователя '{}'", id, user.getUsername());
                return new ResponseEntity<>("Транзакция не найдена", HttpStatus.NOT_FOUND);
            }

            logger.info("Успешно найдена транзакция с ID: {} для пользователя '{}'", id, user.getUsername());
            return new ResponseEntity<>(transaction, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Ошибка при получении транзакции с ID {}: {}", id, e.getMessage(), e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping
    public ResponseEntity<?> createTransaction(
            Authentication authentication,
            @RequestBody Transaction transaction) {

        try {
            User user = userRepository.findByUsername(authentication.getName());
            logger.info("Пользователь '{}' создает новую транзакцию", user.getUsername());

            transaction.setUser(user);

            Optional<TransactionStatus> newStatus = statusRepository.findByCode("NEW");
            newStatus.ifPresent(transaction::setStatus);

            transaction.setCreated(LocalDateTime.now());
            transaction.setUpdated(LocalDateTime.now());

            Transaction savedTransaction = transactionRepository.save(transaction);
            logger.info("Успешно создана транзакция с ID: {} для пользователя '{}'",
                    savedTransaction.getId(), user.getUsername());

            return new ResponseEntity<>(savedTransaction, HttpStatus.CREATED);

        } catch (Exception e) {
            logger.error("Ошибка при создании транзакции: {}", e.getMessage(), e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTransaction(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody Transaction transactionDetails) {

        try {
            User user = userRepository.findByUsername(authentication.getName());
            logger.info("Пользователь '{}' обновляет транзакцию с ID: {}", user.getUsername(), id);

            Transaction transaction = transactionRepository.findByIdAndUserId(id, user.getId());

            if (transaction == null) {
                logger.warn("Транзакция с ID: {} не найдена для пользователя '{}'", id, user.getUsername());
                return new ResponseEntity<>("Транзакция не найдена", HttpStatus.NOT_FOUND);
            }

            if (!transaction.isEditable()) {
                logger.warn("Невозможно редактировать транзакцию с ID: {} в текущем статусе", id);
                return new ResponseEntity<>("Невозможно редактировать транзакцию в текущем статусе", HttpStatus.BAD_REQUEST);
            }

            transaction.setPersonType(transactionDetails.getPersonType());
            transaction.setOperationDateTime(transactionDetails.getOperationDateTime());
            transaction.setComment(transactionDetails.getComment());
            transaction.setAmount(transactionDetails.getAmount());
            transaction.setStatus(transactionDetails.getStatus());
            transaction.setSenderBank(transactionDetails.getSenderBank());
            transaction.setRecipientBank(transactionDetails.getRecipientBank());
            transaction.setRecipientInn(transactionDetails.getRecipientInn());
            transaction.setCategory(transactionDetails.getCategory());
            transaction.setRecipientPhone(transactionDetails.getRecipientPhone());
            transaction.setUpdated(LocalDateTime.now());

            Transaction updatedTransaction = transactionRepository.save(transaction);
            logger.info("Успешно обновлена транзакция с ID: {} для пользователя '{}'", id, user.getUsername());

            return new ResponseEntity<>(updatedTransaction, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Ошибка при обновлении транзакции с ID {}: {}", id, e.getMessage(), e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTransaction(
            Authentication authentication,
            @PathVariable Long id) {

        try {
            User user = userRepository.findByUsername(authentication.getName());
            logger.info("Пользователь '{}' удаляет транзакцию с ID: {}", user.getUsername(), id);

            Transaction transaction = transactionRepository.findByIdAndUserId(id, user.getId());

            if (transaction == null) {
                logger.warn("Транзакция с ID: {} не найдена для пользователя '{}'", id, user.getUsername());
                return new ResponseEntity<>("Транзакция не найдена", HttpStatus.NOT_FOUND);
            }

            if (!transaction.isDeletable()) {
                logger.warn("Невозможно удалить транзакцию с ID: {} в текущем статусе", id);
                return new ResponseEntity<>("Невозможно удалить транзакцию в текущем статусе", HttpStatus.BAD_REQUEST);
            }

            Optional<TransactionStatus> deletedStatus = statusRepository.findByCode("DELETED");
            if (deletedStatus.isPresent()) {
                transaction.setStatus(deletedStatus.get());
                transaction.setUpdated(LocalDateTime.now());
                transactionRepository.save(transaction);
                logger.info("Транзакция с ID: {} успешно помечена как удаленная", id);
                return new ResponseEntity<>("Транзакция помечена как удаленная", HttpStatus.OK);
            } else {
                logger.error("Статус 'DELETED' не найден в системе");
                return new ResponseEntity<>("Статус 'DELETED' не найден", HttpStatus.INTERNAL_SERVER_ERROR);
            }

        } catch (Exception e) {
            logger.error("Ошибка при удалении транзакции с ID {}: {}", id, e.getMessage(), e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/stats/count-by-period")
    public ResponseEntity<?> getTransactionCountByPeriod(
            Authentication authentication,
            @RequestParam String period,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd.MM.yyyy") LocalDateTime baseDate) {

        try {
            User user = userRepository.findByUsername(authentication.getName());
            logger.info("Пользователь '{}' запрашивает статистику по количеству транзакций за период: {}",
                    user.getUsername(), period);

            if (baseDate == null) {
                baseDate = LocalDateTime.now();
                logger.debug("Базовая дата не указана, используется текущая дата: {}", baseDate);
            } else {
                logger.debug("Используется указанная базовая дата: {}", baseDate);
            }

            LocalDateTime startDate;
            LocalDateTime endDate = LocalDateTime.now();

            switch (period.toLowerCase()) {
                case "week":
                    startDate = baseDate.minusWeeks(1);
                    logger.debug("Расчет за неделю: с {} по {}", startDate, endDate);
                    break;
                case "month":
                    startDate = baseDate.minusMonths(1);
                    logger.debug("Расчет за месяц: с {} по {}", startDate, endDate);
                    break;
                case "quarter":
                    startDate = baseDate.minusMonths(3);
                    logger.debug("Расчет за квартал: с {} по {}", startDate, endDate);
                    break;
                case "year":
                    startDate = baseDate.minusYears(1);
                    logger.debug("Расчет за год: с {} по {}", startDate, endDate);
                    break;
                default:
                    logger.warn("Указан неверный период: {}", period);
                    return new ResponseEntity<>("Неверный период", HttpStatus.BAD_REQUEST);
            }

            Long count = transactionRepository.countTransactionsByPeriod(user.getId(), startDate, endDate);
            logger.info("Для пользователя '{}' найдено {} транзакций за период {}",
                    user.getUsername(), count, period);

            Map<String, Object> response = new HashMap<>();
            response.put("period", period);
            response.put("count", count);

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Ошибка при получении статистики по количеству транзакций: {}", e.getMessage(), e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/stats/amount-by-type")
    public ResponseEntity<?> getAmountByTransactionType(
            Authentication authentication,
            @RequestParam String typeCode,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd.MM.yyyy") LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd.MM.yyyy") LocalDateTime endDate) {

        try {
            User user = userRepository.findByUsername(authentication.getName());
            logger.info("Пользователь '{}' запрашивает статистику по суммам транзакций типа: {}",
                    user.getUsername(), typeCode);

            if (startDate == null) {
                startDate = LocalDateTime.now().minusMonths(1);
                logger.debug("Начальная дата не указана, используется дата: {}", startDate);
            }

            if (endDate == null) {
                endDate = LocalDateTime.now();
                logger.debug("Конечная дата не указана, используется текущая дата: {}", endDate);
            }

            logger.debug("Расчет сумм транзакций типа {} за период с {} по {}", typeCode, startDate, endDate);
            BigDecimal totalAmount = transactionRepository.sumAmountByTransactionType(
                    user.getId(), typeCode, startDate, endDate);

            logger.info("Для пользователя '{}' общая сумма транзакций типа {} составляет: {}",
                    user.getUsername(), typeCode, totalAmount != null ? totalAmount : BigDecimal.ZERO);

            Map<String, Object> response = new HashMap<>();
            response.put("typeCode", typeCode);
            response.put("totalAmount", totalAmount != null ? totalAmount : BigDecimal.ZERO);

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Ошибка при получении статистики по суммам транзакций типа {}: {}",
                    typeCode, e.getMessage(), e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/stats/income-vs-expense")
    public ResponseEntity<?> getIncomeVsExpense(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd.MM.yyyy") LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd.MM.yyyy") LocalDateTime endDate) {

        try {
            User user = userRepository.findByUsername(authentication.getName());
            logger.info("Пользователь '{}' запрашивает сравнение доходов и расходов", user.getUsername());

            if (startDate == null) {
                startDate = LocalDateTime.now().minusMonths(1);
                logger.debug("Начальная дата не указана, используется дата: {}", startDate);
            }

            if (endDate == null) {
                endDate = LocalDateTime.now();
                logger.debug("Конечная дата не указана, используется текущая дата: {}", endDate);
            }

            logger.debug("Расчет доходов и расходов за период с {} по {}", startDate, endDate);
            BigDecimal totalIncome = transactionRepository.sumAmountByTransactionType(
                    user.getId(), TransactionType.INCOME, startDate, endDate);

            BigDecimal totalExpense = transactionRepository.sumAmountByTransactionType(
                    user.getId(), TransactionType.EXPENSE, startDate, endDate);

            BigDecimal balance = (totalIncome != null ? totalIncome : BigDecimal.ZERO)
                    .subtract(totalExpense != null ? totalExpense : BigDecimal.ZERO);

            logger.info("Для пользователя '{}' за период с {} по {}: доходы = {}, расходы = {}, баланс = {}",
                    user.getUsername(), startDate, endDate,
                    totalIncome != null ? totalIncome : BigDecimal.ZERO,
                    totalExpense != null ? totalExpense : BigDecimal.ZERO,
                    balance);

            Map<String, Object> response = new HashMap<>();
            response.put("totalIncome", totalIncome != null ? totalIncome : BigDecimal.ZERO);
            response.put("totalExpense", totalExpense != null ? totalExpense : BigDecimal.ZERO);
            response.put("balance", balance);

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Ошибка при сравнении доходов и расходов: {}", e.getMessage(), e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/stats/count-by-status")
    public ResponseEntity<?> getTransactionCountByStatus(Authentication authentication) {
        try {
            User user = userRepository.findByUsername(authentication.getName());
            logger.info("Пользователь '{}' запрашивает количество транзакций по статусам", user.getUsername());

            List<Object[]> statusCounts = transactionRepository.countTransactionsByStatus(user.getId());
            logger.debug("Получены данные о количестве транзакций в разных статусах: {} записей", statusCounts.size());

            Map<String, Object> response = new HashMap<>();
            for (Object[] result : statusCounts) {
                response.put((String) result[0], result[1]);
                logger.debug("Статус '{}': {} транзакций", result[0], result[1]);
            }

            logger.info("Успешно собрана статистика по статусам транзакций для пользователя '{}'", user.getUsername());
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Ошибка при получении статистики по статусам транзакций: {}", e.getMessage(), e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/stats/count-by-bank")
    public ResponseEntity<?> getTransactionCountByBank(Authentication authentication) {
        try {
            User user = userRepository.findByUsername(authentication.getName());
            logger.info("Пользователь '{}' запрашивает количество транзакций по банкам", user.getUsername());

            List<Object[]> senderBankCounts = transactionRepository.countTransactionsBySenderBank(user.getId());
            List<Object[]> recipientBankCounts = transactionRepository.countTransactionsByRecipientBank(user.getId());

            logger.debug("Получены данные о количестве транзакций по банкам-отправителям: {} записей",
                    senderBankCounts.size());
            logger.debug("Получены данные о количестве транзакций по банкам-получателям: {} записей",
                    recipientBankCounts.size());

            Map<String, Object> response = new HashMap<>();

            Map<String, Long> senderBanks = new HashMap<>();
            for (Object[] result : senderBankCounts) {
                senderBanks.put((String) result[0], (Long) result[1]);
                logger.debug("Банк-отправитель '{}': {} транзакций", result[0], result[1]);
            }

            Map<String, Long> recipientBanks = new HashMap<>();
            for (Object[] result : recipientBankCounts) {
                recipientBanks.put((String) result[0], (Long) result[1]);
                logger.debug("Банк-получатель '{}': {} транзакций", result[0], result[1]);
            }

            response.put("senderBanks", senderBanks);
            response.put("recipientBanks", recipientBanks);

            logger.info("Успешно собрана статистика по банкам для пользователя '{}'", user.getUsername());
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Ошибка при получении статистики по банкам: {}", e.getMessage(), e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/stats/amount-by-category")
    public ResponseEntity<?> getAmountByCategory(
            Authentication authentication,
            @RequestParam String typeCode) {

        try {
            User user = userRepository.findByUsername(authentication.getName());
            logger.info("Пользователь '{}' запрашивает суммы транзакций по категориям для типа: {}",
                    user.getUsername(), typeCode);

            List<Object[]> categorySums = transactionRepository.sumAmountByCategory(user.getId(), typeCode);
            logger.debug("Получены данные о суммах транзакций по {} категориям для типа {}",
                    categorySums.size(), typeCode);

            Map<String, BigDecimal> categoryMap = new HashMap<>();
            for (Object[] result : categorySums) {
                categoryMap.put((String) result[0], (BigDecimal) result[1]);
                logger.debug("Категория '{}': сумма {}", result[0], result[1]);
            }

            logger.info("Успешно собрана статистика по суммам в категориях для пользователя '{}'", user.getUsername());
            return new ResponseEntity<>(categoryMap, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Ошибка при получении статистики по категориям для типа {}: {}",
                    typeCode, e.getMessage(), e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}