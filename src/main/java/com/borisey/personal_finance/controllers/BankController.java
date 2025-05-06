package com.borisey.personal_finance.controllers;

import com.borisey.personal_finance.models.Bank;
import com.borisey.personal_finance.repo.BankRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/banks")
@PreAuthorize("hasRole('ADMIN')")
public class BankController {

    private static final Logger logger = LoggerFactory.getLogger(BankController.class);
    private final BankRepository bankRepository;

    @Autowired
    public BankController(BankRepository bankRepository) {
        this.bankRepository = bankRepository;
        logger.info("Инициализирован контроллер банков");
    }

    @GetMapping
    public ResponseEntity<List<Bank>> getAllBanks(@RequestParam(defaultValue = "title") String sortBy) {
        logger.info("Запрос на получение всех банков с сортировкой по полю '{}'", sortBy);
        try {
            List<Bank> banks = bankRepository.findAll(Sort.by(Sort.Direction.ASC, sortBy));
            logger.info("Успешно получен список из {} банков", banks.size());
            return new ResponseEntity<>(banks, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Ошибка при получении списка банков: {}", e.getMessage(), e);
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBankById(@PathVariable Long id) {
        logger.info("Запрос на получение банка с ID: {}", id);
        try {
            return bankRepository.findById(id)
                    .map(bank -> {
                        logger.info("Успешно найден банк: {}", bank.getTitle());
                        return new ResponseEntity<Bank>(bank, HttpStatus.OK);
                    })
                    .orElseGet(() -> {
                        logger.warn("Банк с ID: {} не найден", id);
                        return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
                    });
        } catch (Exception e) {
            logger.error("Ошибка при получении банка с ID {}: {}", id, e.getMessage(), e);
            return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<Bank>> searchBanks(@RequestParam String term) {
        logger.info("Запрос на поиск банков по термину: '{}'", term);
        try {
            List<Bank> banks = bankRepository.searchByTitle(term);
            logger.info("Найдено {} банков по запросу '{}'", banks.size(), term);
            return new ResponseEntity<>(banks, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Ошибка при поиске банков по термину '{}': {}", term, e.getMessage(), e);
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping
    public ResponseEntity<?> createBank(@RequestBody Bank bank) {
        logger.info("Запрос на создание нового банка: {}", bank.getTitle());
        try {
            Bank savedBank = bankRepository.save(bank);
            logger.info("Успешно создан новый банк с ID: {}", savedBank.getId());
            return new ResponseEntity<Bank>(savedBank, HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error("Ошибка при создании банка '{}': {}", bank.getTitle(), e.getMessage(), e);
            return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateBank(@PathVariable Long id, @RequestBody Bank bankDetails) {
        logger.info("Запрос на обновление банка с ID: {}", id);
        try {
            return bankRepository.findById(id)
                    .map(bank -> {
                        bank.setTitle(bankDetails.getTitle());
                        bank.setBik(bankDetails.getBik());
                        Bank updatedBank = bankRepository.save(bank);
                        logger.info("Успешно обновлен банк с ID: {}, новое название: {}", id, updatedBank.getTitle());
                        return new ResponseEntity<Bank>(updatedBank, HttpStatus.OK);
                    })
                    .orElseGet(() -> {
                        logger.warn("Банк с ID: {} не найден при попытке обновления", id);
                        return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
                    });
        } catch (Exception e) {
            logger.error("Ошибка при обновлении банка с ID {}: {}", id, e.getMessage(), e);
            return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBank(@PathVariable Long id) {
        logger.info("Запрос на удаление банка с ID: {}", id);
        try {
            return bankRepository.findById(id)
                    .map(bank -> {
                        bankRepository.delete(bank);
                        logger.info("Успешно удален банк с ID: {}, название: {}", id, bank.getTitle());
                        return new ResponseEntity<String>("Банк успешно удален", HttpStatus.OK);
                    })
                    .orElseGet(() -> {
                        logger.warn("Банк с ID: {} не найден при попытке удаления", id);
                        return new ResponseEntity<String>("Банк не найден", HttpStatus.NOT_FOUND);
                    });
        } catch (Exception e) {
            logger.error("Ошибка при удалении банка с ID {}: {}", id, e.getMessage(), e);
            return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}