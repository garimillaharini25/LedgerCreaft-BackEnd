package com.expensetracker.expense;

import com.expensetracker.budget.BudgetService;
import com.expensetracker.category.Category;
import com.expensetracker.category.CategoryService;
import com.expensetracker.common.ResourceNotFoundException;
import com.expensetracker.user.User;
import com.expensetracker.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final CategoryService categoryService;
    private final UserRepository userRepository;
    private final BudgetService budgetService;

    @Transactional(readOnly = true)
    public ExpensePageResponse getExpenses(
            Long userId,
            LocalDate startDate,
            LocalDate endDate,
            Long categoryId,
            PaymentMethod paymentMethod,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            String search,
            int page,
            int size,
            String sortStr) {

        Sort sort = resolveSort(sortStr);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), sort);

        Specification<Expense> spec = ExpenseSpecification.filterExpenses(
                userId, startDate, endDate, categoryId, paymentMethod, minAmount, maxAmount, search);

        Page<Expense> expensePage = expenseRepository.findAll(spec, pageable);
        return ExpensePageResponse.fromPage(expensePage);
    }

    @Transactional(readOnly = true)
    public ExpenseDto getExpenseById(Long userId, Long id) {
        Expense expense = expenseRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));

        return ExpenseDto.fromEntity(expense);
    }

    @Transactional
    public ExpenseDto createExpense(Long userId, CreateExpenseRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Category category = categoryService.getCategoryEntity(request.getCategoryId(), userId);

        Expense expense = Expense.builder()
                .user(user)
                .amount(request.getAmount())
                .description(request.getDescription().trim())
                .category(category)
                .expenseDate(request.getExpenseDate())
                .paymentMethod(request.getPaymentMethod())
                .notes(request.getNotes() != null ? request.getNotes().trim() : null)
                .build();

        Expense saved = expenseRepository.save(expense);

        // Check budget alerts
        budgetService.checkAndCreateAlerts(userId, saved.getExpenseDate().getMonthValue(), saved.getExpenseDate().getYear());

        return ExpenseDto.fromEntity(saved);
    }

    @Transactional
    public ExpenseDto updateExpense(Long userId, Long id, UpdateExpenseRequest request) {
        Expense expense = expenseRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));

        LocalDate oldDate = expense.getExpenseDate();
        Category category = categoryService.getCategoryEntity(request.getCategoryId(), userId);

        expense.setAmount(request.getAmount());
        expense.setDescription(request.getDescription().trim());
        expense.setCategory(category);
        expense.setExpenseDate(request.getExpenseDate());
        expense.setPaymentMethod(request.getPaymentMethod());
        expense.setNotes(request.getNotes() != null ? request.getNotes().trim() : null);

        Expense updated = expenseRepository.save(expense);

        // Check alerts for new date
        budgetService.checkAndCreateAlerts(userId, updated.getExpenseDate().getMonthValue(), updated.getExpenseDate().getYear());

        // If date moved to a different month/year, check alerts for old month too
        if (oldDate.getMonthValue() != updated.getExpenseDate().getMonthValue() || oldDate.getYear() != updated.getExpenseDate().getYear()) {
            budgetService.checkAndCreateAlerts(userId, oldDate.getMonthValue(), oldDate.getYear());
        }

        return ExpenseDto.fromEntity(updated);
    }

    @Transactional
    public void deleteExpense(Long userId, Long id) {
        Expense expense = expenseRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));

        LocalDate date = expense.getExpenseDate();
        expenseRepository.delete(expense);

        // Recheck budget alerts after deletion
        budgetService.checkAndCreateAlerts(userId, date.getMonthValue(), date.getYear());
    }

    private Sort resolveSort(String sortStr) {
        if (sortStr == null) return Sort.by(Sort.Direction.DESC, "expenseDate").and(Sort.by(Sort.Direction.DESC, "id"));

        return switch (sortStr.trim().toLowerCase()) {
            case "oldest" -> Sort.by(Sort.Direction.ASC, "expenseDate").and(Sort.by(Sort.Direction.ASC, "id"));
            case "highest" -> Sort.by(Sort.Direction.DESC, "amount");
            case "lowest" -> Sort.by(Sort.Direction.ASC, "amount");
            case "description a-z", "description_asc" -> Sort.by(Sort.Direction.ASC, "description");
            default -> Sort.by(Sort.Direction.DESC, "expenseDate").and(Sort.by(Sort.Direction.DESC, "id"));
        };
    }
}
