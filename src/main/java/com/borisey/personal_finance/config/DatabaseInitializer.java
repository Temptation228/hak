package com.borisey.personal_finance.config;

import com.borisey.personal_finance.models.*;
import com.borisey.personal_finance.repo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final PersonTypeRepository personTypeRepository;
    private final TransactionTypeRepository transactionTypeRepository;
    private final TransactionStatusRepository transactionStatusRepository;
    private final BankRepository bankRepository;

    @Autowired
    public DatabaseInitializer(
            PersonTypeRepository personTypeRepository,
            TransactionTypeRepository transactionTypeRepository,
            TransactionStatusRepository transactionStatusRepository,
            BankRepository bankRepository) {
        this.personTypeRepository = personTypeRepository;
        this.transactionTypeRepository = transactionTypeRepository;
        this.transactionStatusRepository = transactionStatusRepository;
        this.bankRepository = bankRepository;
    }

    @Override
    public void run(String... args) {
        // Инициализация типов лиц
        if (personTypeRepository.count() == 0) {
            personTypeRepository.saveAll(Arrays.asList(
                    new PersonType(PersonType.INDIVIDUAL, "Физическое лицо"),
                    new PersonType(PersonType.LEGAL, "Юридическое лицо")
            ));
        }

        // Инициализация типов транзакций
        if (transactionTypeRepository.count() == 0) {
            transactionTypeRepository.saveAll(Arrays.asList(
                    new TransactionType(TransactionType.INCOME, "Поступление"),
                    new TransactionType(TransactionType.EXPENSE, "Списание"),
                    new TransactionType(TransactionType.TRANSFER, "Перевод")
            ));
        }

        // Инициализация статусов транзакций
        if (transactionStatusRepository.count() == 0) {
            transactionStatusRepository.saveAll(Arrays.asList(
                    new TransactionStatus("NEW", "Новая"),
                    new TransactionStatus("CONFIRMED", "Подтвержденная"),
                    new TransactionStatus("PROCESSING", "В обработке"),
                    new TransactionStatus("CANCELLED", "Отменена"),
                    new TransactionStatus("COMPLETED", "Платеж выполнен"),
                    new TransactionStatus("DELETED", "Платеж удален"),
                    new TransactionStatus("RETURNED", "Возврат")
            ));
        }

        // Инициализация банков
        if (bankRepository.count() == 0) {
            bankRepository.saveAll(Arrays.asList(
                    new Bank("Сбербанк", "044525225"),
                    new Bank("ВТБ", "044525187"),
                    new Bank("Альфа-Банк", "044525593"),
                    new Bank("Тинькофф Банк", "044525974"),
                    new Bank("Газпромбанк", "044525823")
            ));
        }
    }
}