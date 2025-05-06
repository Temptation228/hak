package com.borisey.personal_finance;

import com.borisey.personal_finance.models.PersonType;
import com.borisey.personal_finance.models.TransactionType;
import com.borisey.personal_finance.repo.PersonTypeRepository;
import com.borisey.personal_finance.repo.TransactionTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PersonalFinanceApplication implements CommandLineRunner {

	@Autowired
	private TransactionTypeRepository transactionTypeRepository;

	@Autowired
	private PersonTypeRepository personTypeRepository;

	public static void main(String[] args) {
		SpringApplication.run(PersonalFinanceApplication.class, args);
	}

	@Override
	public void run(String... args) {
		if (transactionTypeRepository.count() == 0) {
			TransactionType incomeType = new TransactionType();
			incomeType.setId(1L);
			incomeType.setTitle("Доход");
			incomeType.setCode("INCOME");
			transactionTypeRepository.save(incomeType);

			TransactionType expenseType = new TransactionType();
			expenseType.setId(2L);
			expenseType.setTitle("Расход");
			expenseType.setCode("EXPENSE");
			transactionTypeRepository.save(expenseType);
		}

		if (personTypeRepository.count() == 0) {
			PersonType personType1 = new PersonType();
			personType1.setId(1L);
			personType1.setTitle("Физическое лицо");
			personTypeRepository.save(personType1);

			PersonType personType2 = new PersonType();
			personType2.setId(2L);
			personType2.setTitle("Юридическое лицо");
			personTypeRepository.save(personType2);
		}
	}
}
