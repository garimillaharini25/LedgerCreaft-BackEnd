package com.expensetracker.category;

import com.expensetracker.common.BadRequestException;
import com.expensetracker.common.DuplicateResourceException;
import com.expensetracker.common.ForbiddenException;
import com.expensetracker.common.ResourceNotFoundException;
import com.expensetracker.expense.Expense;
import com.expensetracker.expense.ExpenseRepository;
import com.expensetracker.user.User;
import com.expensetracker.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final com.expensetracker.budget.BudgetCategoryRepository budgetCategoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryDto> getAllCategoriesWithStats(Long userId) {
        List<Category> categories = categoryRepository.findAllAvailableForUser(userId);
        List<Expense> expenses = expenseRepository.findByUserId(userId);

        Map<Long, BigDecimal> categorySpendingMap = new HashMap<>();
        Map<Long, Long> categoryCountMap = new HashMap<>();

        for (Expense expense : expenses) {
            Long catId = expense.getCategory().getId();
            categorySpendingMap.put(catId, categorySpendingMap.getOrDefault(catId, BigDecimal.ZERO).add(expense.getAmount()));
            categoryCountMap.put(catId, categoryCountMap.getOrDefault(catId, 0L) + 1L);
        }

        return categories.stream()
                .map(cat -> CategoryDto.fromEntity(
                        cat,
                        categorySpendingMap.getOrDefault(cat.getId(), BigDecimal.ZERO),
                        categoryCountMap.getOrDefault(cat.getId(), 0L)
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Category getCategoryEntity(Long categoryId, Long userId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));

        if (!category.isSystemCategory() && (category.getUser() == null || !category.getUser().getId().equals(userId))) {
            throw new ResourceNotFoundException("Category not found with id: " + categoryId);
        }

        return category;
    }

    @Transactional
    public CategoryDto createCategory(Long userId, CreateCategoryRequest request) {
        String trimmedName = request.getName().trim();

        if (categoryRepository.existsByNameIgnoreCaseAndSystemCategoryTrue(trimmedName) ||
            categoryRepository.existsByNameIgnoreCaseAndUserId(trimmedName, userId)) {
            throw new DuplicateResourceException("Category with name '" + trimmedName + "' already exists");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Category category = Category.builder()
                .name(trimmedName)
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .icon(request.getIcon().trim())
                .color(request.getColor().trim())
                .user(user)
                .systemCategory(false)
                .build();

        Category saved = categoryRepository.save(category);
        return CategoryDto.fromEntity(saved, BigDecimal.ZERO, 0L);
    }

    @Transactional
    public CategoryDto updateCategory(Long userId, Long categoryId, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));

        if (category.isSystemCategory()) {
            throw new ForbiddenException("System categories cannot be edited");
        }

        if (category.getUser() == null || !category.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Category not found with id: " + categoryId);
        }

        String trimmedName = request.getName().trim();
        if (!category.getName().equalsIgnoreCase(trimmedName)) {
            if (categoryRepository.existsByNameIgnoreCaseAndSystemCategoryTrue(trimmedName) ||
                categoryRepository.existsByNameIgnoreCaseAndUserId(trimmedName, userId)) {
                throw new DuplicateResourceException("Category with name '" + trimmedName + "' already exists");
            }
        }

        category.setName(trimmedName);
        category.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        category.setIcon(request.getIcon().trim());
        category.setColor(request.getColor().trim());

        Category updated = categoryRepository.save(category);

        List<Expense> expenses = expenseRepository.findByUserId(userId);
        BigDecimal totalSpend = expenses.stream()
                .filter(e -> e.getCategory().getId().equals(updated.getId()))
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long count = expenses.stream()
                .filter(e -> e.getCategory().getId().equals(updated.getId()))
                .count();

        return CategoryDto.fromEntity(updated, totalSpend, count);
    }

    @Transactional
    public void deleteCategory(Long userId, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));

        if (category.isSystemCategory()) {
            throw new ForbiddenException("System categories cannot be deleted");
        }

        if (category.getUser() == null || !category.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Category not found with id: " + categoryId);
        }

        Category otherCategory = categoryRepository.findByNameAndSystemCategoryTrue("Other")
                .orElseThrow(() -> new BadRequestException("Default 'Other' category not found for reassignment"));

        expenseRepository.reassignCategory(categoryId, otherCategory.getId(), userId);
        budgetCategoryRepository.deleteByCategoryId(categoryId);
        categoryRepository.delete(category);
    }
}
