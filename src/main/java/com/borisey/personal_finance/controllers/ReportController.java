package com.borisey.personal_finance.controllers;

import com.borisey.personal_finance.models.Transaction;
import com.borisey.personal_finance.models.User;
import com.borisey.personal_finance.repo.UserRepository;
import com.borisey.personal_finance.services.ReportService;
import com.borisey.personal_finance.specifications.TransactionSpecifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.format.DateTimeFormatter;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasRole('USER')")
public class ReportController {
    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);
    private final ReportService reportService;
    private final UserRepository userRepository;

    @Autowired
    public ReportController(
            ReportService reportService,
            UserRepository userRepository) {
        this.reportService = reportService;
        this.userRepository = userRepository;
        logger.info("Инициализирован контроллер отчетов");
    }

    @GetMapping("/transactions/excel")
    public ResponseEntity<byte[]> exportTransactionsToExcel(
            Authentication authentication,
            @RequestParam(required = false) Long senderBankId,
            @RequestParam(required = false) Long recipientBankId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Long statusId,
            @RequestParam(required = false) String inn,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) Long transactionTypeId,
            @RequestParam(required = false) Long categoryId) {

        try {
            User user = userRepository.findByUsername(authentication.getName());
            logger.info("Пользователь '{}' запрашивает экспорт транзакций в Excel", user.getUsername());

            LocalDateTime effectiveEndDate = endDate != null ? endDate : LocalDateTime.now();
            LocalDateTime effectiveStartDate = startDate != null ? startDate : effectiveEndDate.minusMonths(1);

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
                spec = spec.and(TransactionSpecifications.dateIsBetween(effectiveStartDate, effectiveEndDate));
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

            byte[] excelBytes = reportService.generateTransactionsReport(user, spec, effectiveStartDate, effectiveEndDate);

            String filename = "transactions_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            headers.setContentLength(excelBytes.length);

            logger.info("Файл Excel успешно сформирован для пользователя '{}'", user.getUsername());
            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);

        } catch (IOException e) {
            logger.error("Ошибка при формировании Excel отчета: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        } catch (Exception e) {
            logger.error("Неожиданная ошибка при формировании Excel отчета: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/categories/excel")
    public ResponseEntity<byte[]> exportCategoryReportToExcel(
            Authentication authentication,
            @RequestParam String typeCode) {

        try {
            User user = userRepository.findByUsername(authentication.getName());
            logger.info("Пользователь '{}' запрашивает экспорт отчета по категориям типа {} в Excel",
                    user.getUsername(), typeCode);

            byte[] excelBytes = reportService.generateCategoryReportByType(user, typeCode);

            String fileType = typeCode.equals("INCOME") ? "income" : "expense";
            String filename = "categories_" + fileType + "_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            headers.setContentLength(excelBytes.length);

            logger.info("Файл Excel по категориям успешно сформирован для пользователя '{}'", user.getUsername());
            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);

        } catch (IOException e) {
            logger.error("Ошибка при формировании Excel отчета по категориям: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        } catch (Exception e) {
            logger.error("Неожиданная ошибка при формировании Excel отчета по категориям: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/dashboard/excel")
    public ResponseEntity<byte[]> exportDashboardToExcel(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        try {
            User user = userRepository.findByUsername(authentication.getName());
            logger.info("Пользователь '{}' запрашивает экспорт сводного отчета в Excel", user.getUsername());

            if (startDate == null) {
                startDate = LocalDateTime.now().minusYears(1);
            }
            if (endDate == null) {
                endDate = LocalDateTime.now();
            }

            byte[] excelBytes = reportService.generateDashboardReport(user, startDate, endDate);

            String filename = "financial_dashboard_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            headers.setContentLength(excelBytes.length);

            logger.info("Сводный отчет Excel успешно сформирован для пользователя '{}'", user.getUsername());
            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);

        } catch (IOException e) {
            logger.error("Ошибка при формировании сводного Excel отчета: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        } catch (Exception e) {
            logger.error("Неожиданная ошибка при формировании сводного Excel отчета: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}

