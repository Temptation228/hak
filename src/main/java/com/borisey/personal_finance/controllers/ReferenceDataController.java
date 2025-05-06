package com.borisey.personal_finance.controllers;

import com.borisey.personal_finance.models.PersonType;
import com.borisey.personal_finance.models.TransactionStatus;
import com.borisey.personal_finance.models.TransactionType;
import com.borisey.personal_finance.repo.PersonTypeRepository;
import com.borisey.personal_finance.repo.TransactionStatusRepository;
import com.borisey.personal_finance.repo.TransactionTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reference")
@PreAuthorize("hasRole('USER')")
public class ReferenceDataController {

    private static final Logger logger = LoggerFactory.getLogger(ReferenceDataController.class);
    private final PersonTypeRepository personTypeRepository;
    private final TransactionTypeRepository transactionTypeRepository;
    private final TransactionStatusRepository transactionStatusRepository;

    @Autowired
    public ReferenceDataController(
            PersonTypeRepository personTypeRepository,
            TransactionTypeRepository transactionTypeRepository,
            TransactionStatusRepository transactionStatusRepository) {
        this.personTypeRepository = personTypeRepository;
        this.transactionTypeRepository = transactionTypeRepository;
        this.transactionStatusRepository = transactionStatusRepository;
        logger.info("Инициализирован контроллер справочных данных");
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllReferenceData() {
        logger.info("Запрос на получение всех справочных данных");
        try {
            Map<String, Object> referenceData = new HashMap<>();

            List<PersonType> personTypes = personTypeRepository.findAll();
            List<TransactionType> transactionTypes = transactionTypeRepository.findAll();
            List<TransactionStatus> transactionStatuses = transactionStatusRepository.findAll();

            referenceData.put("personTypes", personTypes);
            referenceData.put("transactionTypes", transactionTypes);
            referenceData.put("transactionStatuses", transactionStatuses);

            logger.info("Успешно получены справочные данные: {} типов лиц, {} типов транзакций, {} статусов транзакций",
                    personTypes.size(), transactionTypes.size(), transactionStatuses.size());
            return new ResponseEntity<>(referenceData, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Ошибка при получении справочных данных: {}", e.getMessage(), e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/person-types")
    public ResponseEntity<?> getAllPersonTypes() {
        logger.info("Запрос на получение всех типов лиц");
        try {
            List<PersonType> personTypes = personTypeRepository.findAll();
            logger.info("Успешно получено {} типов лиц", personTypes.size());
            return new ResponseEntity<>(personTypes, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Ошибка при получении типов лиц: {}", e.getMessage(), e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/transaction-types")
    public ResponseEntity<?> getAllTransactionTypes() {
        logger.info("Запрос на получение всех типов транзакций");
        try {
            List<TransactionType> transactionTypes = transactionTypeRepository.findAll();
            logger.info("Успешно получено {} типов транзакций", transactionTypes.size());
            return new ResponseEntity<>(transactionTypes, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Ошибка при получении типов транзакций: {}", e.getMessage(), e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/transaction-statuses")
    public ResponseEntity<?> getAllTransactionStatuses() {
        logger.info("Запрос на получение всех статусов транзакций");
        try {
            List<TransactionStatus> transactionStatuses = transactionStatusRepository.findAll();
            logger.info("Успешно получено {} статусов транзакций", transactionStatuses.size());
            return new ResponseEntity<>(transactionStatuses, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Ошибка при получении статусов транзакций: {}", e.getMessage(), e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
