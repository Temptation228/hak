package com.borisey.personal_finance.controllers;

import com.borisey.personal_finance.models.Category;
import com.borisey.personal_finance.models.User;
import com.borisey.personal_finance.repo.CategoryRepository;
import com.borisey.personal_finance.repo.TransactionTypeRepository;
import com.borisey.personal_finance.repo.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
@PreAuthorize("hasRole('USER')")
public class CategoryController {

    private static final Logger logger = LoggerFactory.getLogger(CategoryController.class);
    private final CategoryRepository categoryRepository;
    private final TransactionTypeRepository transactionTypeRepository;
    private final UserRepository userRepository;

    @Autowired
    public CategoryController(
            CategoryRepository categoryRepository,
            TransactionTypeRepository transactionTypeRepository,
            UserRepository userRepository) {
        this.categoryRepository = categoryRepository;
        this.transactionTypeRepository = transactionTypeRepository;
        this.userRepository = userRepository;
        logger.info("Инициализирован контроллер категорий");
    }

    @GetMapping
    public ResponseEntity<?> getAllCategories(
            Authentication authentication,
            @RequestParam(required = false) Long transactionTypeId,
            @RequestParam(defaultValue = "title") String sortBy) {

        String username = authentication.getName();
        logger.info("Пользователь '{}' запрашивает список категорий с сортировкой по '{}'", username, sortBy);
        if (transactionTypeId != null) {
            logger.info("Запрос с фильтром по типу транзакции ID: {}", transactionTypeId);
        }

        try {
            User user = userRepository.findByUsername(username);
            List<Category> categories;

            if (transactionTypeId != null) {
                categories = categoryRepository.findByUserIdAndTransactionTypeId(
                        user.getId(), transactionTypeId, Sort.by(Sort.Direction.ASC, sortBy));
            } else {
                categories = categoryRepository.findByUserId(
                        user.getId(), Sort.by(Sort.Direction.ASC, sortBy));
            }

            logger.info("Успешно получен список из {} категорий для пользователя '{}'", categories.size(), username);
            return new ResponseEntity<>(categories, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Ошибка при получении списка категорий для пользователя '{}': {}", username, e.getMessage(), e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCategoryById(
            Authentication authentication,
            @PathVariable Long id) {

        String username = authentication.getName();
        logger.info("Пользователь '{}' запрашивает категорию с ID: {}", username, id);

        try {
            User user = userRepository.findByUsername(username);
            Category category = categoryRepository.findByIdAndUserId(id, user.getId());

            if (category == null) {
                logger.warn("Категория с ID: {} не найдена для пользователя '{}'", id, username);
                return new ResponseEntity<>("Категория не найдена", HttpStatus.NOT_FOUND);
            }

            logger.info("Успешно найдена категория '{}' с ID: {} для пользователя '{}'", category.getTitle(), id, username);
            return new ResponseEntity<>(category, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Ошибка при получении категории с ID {} для пользователя '{}': {}", id, username, e.getMessage(), e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/with-totals")
    public ResponseEntity<?> getCategoriesWithTotals(
            Authentication authentication,
            @RequestParam Long transactionTypeId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd.MM.yyyy") LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd.MM.yyyy") LocalDateTime endDate,
            @RequestParam(defaultValue = "title") String sortBy) {

        String username = authentication.getName();
        logger.info("Пользователь '{}' запрашивает категории с итогами, тип транзакции: {}", username, transactionTypeId);

        if (startDate != null) {
            logger.info("Начальная дата: {}", startDate);
        }
        if (endDate != null) {
            logger.info("Конечная дата: {}", endDate);
        }

        try {
            User user = userRepository.findByUsername(username);

            if (startDate == null) {
                startDate = LocalDateTime.now().minusMonths(1);
                logger.info("Установлена дефолтная начальная дата: {}", startDate);
            }

            if (endDate == null) {
                endDate = LocalDateTime.now();
                logger.info("Установлена дефолтная конечная дата: {}", endDate);
            }

            List<Object[]> categoriesWithTotals = categoryRepository.findCategoriesWithTotalAmount(
                    user.getId(),
                    transactionTypeId,
                    startDate,
                    endDate,
                    Sort.by(Sort.Direction.ASC, sortBy));

            Map<String, Object> result = new HashMap<>();
            for (Object[] row : categoriesWithTotals) {
                Category category = (Category) row[0];
                result.put(category.getTitle(), row[1]);
            }

            logger.info("Успешно получены итоги по {} категориям для пользователя '{}'", result.size(), username);
            return new ResponseEntity<>(result, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Ошибка при получении итогов по категориям для пользователя '{}': {}", username, e.getMessage(), e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping
    public ResponseEntity<?> createCategory(
            Authentication authentication,
            @RequestBody Category category) {

        String username = authentication.getName();
        logger.info("Пользователь '{}' создает новую категорию: {}", username, category.getTitle());

        try {
            User user = userRepository.findByUsername(username);
            category.setUser(user);

            category.setCreated(LocalDateTime.now());
            category.setUpdated(LocalDateTime.now());

            Category savedCategory = categoryRepository.save(category);
            logger.info("Успешно создана новая категория '{}' с ID: {} для пользователя '{}'",
                    savedCategory.getTitle(), savedCategory.getId(), username);
            return new ResponseEntity<>(savedCategory, HttpStatus.CREATED);

        } catch (Exception e) {
            logger.error("Ошибка при создании категории '{}' для пользователя '{}': {}",
                    category.getTitle(), username, e.getMessage(), e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody Category categoryDetails) {

        String username = authentication.getName();
        logger.info("Пользователь '{}' обновляет категорию с ID: {}", username, id);

        try {
            User user = userRepository.findByUsername(username);

            Category category = categoryRepository.findByIdAndUserId(id, user.getId());

            if (category == null) {
                logger.warn("Категория с ID: {} не найдена для пользователя '{}'", id, username);
                return new ResponseEntity<>("Категория не найдена", HttpStatus.NOT_FOUND);
            }

            category.setTitle(categoryDetails.getTitle());
            category.setBudget(categoryDetails.getBudget());
            category.setTransactionType(categoryDetails.getTransactionType());
            category.setUpdated(LocalDateTime.now());

            Category updatedCategory = categoryRepository.save(category);
            logger.info("Успешно обновлена категория '{}' с ID: {} для пользователя '{}'",
                    updatedCategory.getTitle(), updatedCategory.getId(), username);
            return new ResponseEntity<>(updatedCategory, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Ошибка при обновлении категории с ID {} для пользователя '{}': {}",
                    id, username, e.getMessage(), e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(
            Authentication authentication,
            @PathVariable Long id) {

        String username = authentication.getName();
        logger.info("Пользователь '{}' удаляет категорию с ID: {}", username, id);

        try {
            User user = userRepository.findByUsername(username);

            Category category = categoryRepository.findByIdAndUserId(id, user.getId());

            if (category == null) {
                logger.warn("Категория с ID: {} не найдена для пользователя '{}'", id, username);
                return new ResponseEntity<>("Категория не найдена", HttpStatus.NOT_FOUND);
            }

            categoryRepository.delete(category);
            logger.info("Успешно удалена категория '{}' с ID: {} для пользователя '{}'",
                    category.getTitle(), id, username);
            return new ResponseEntity<>("Категория успешно удалена", HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Ошибка при удалении категории с ID {} для пользователя '{}': {}",
                    id, username, e.getMessage(), e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchCategories(
            Authentication authentication,
            @RequestParam String term) {

        String username = authentication.getName();
        logger.info("Пользователь '{}' выполняет поиск категорий по запросу: '{}'", username, term);

        try {
            User user = userRepository.findByUsername(username);

            List<Category> categories = categoryRepository.searchByTitle(user.getId(), term);

            logger.info("Успешно найдено {} категорий по запросу '{}' для пользователя '{}'",
                    categories.size(), term, username);
            return new ResponseEntity<>(categories, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Ошибка при поиске категорий для пользователя '{}' по запросу '{}': {}",
                    username, term, e.getMessage(), e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
