package com.expensetracker.budget;

import com.expensetracker.category.Category;
import com.expensetracker.category.CategoryRepository;
import com.expensetracker.common.BadRequestException;
import com.expensetracker.common.DuplicateResourceException;
import com.expensetracker.common.ResourceNotFoundException;
import com.expensetracker.expense.Expense;
import com.expensetracker.expense.ExpenseRepository;
import com.expensetracker.notification.NotificationService;
import com.expensetracker.notification.NotificationType;
import com.expensetracker.user.User;
import com.expensetracker.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final MonthlyBudgetRepository budgetRepository;
    private final BudgetCategoryRepository budgetCategoryRepository;
    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<MonthlyBudgetDto> getAllBudgets(Long userId) {
        List<MonthlyBudget> budgets = budgetRepository.findAllByUserIdOrderByYearDescMonthDesc(userId);
        return budgets.stream()
                .map(b -> mapToDto(b, userId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MonthlyBudgetDto getBudgetByYearAndMonth(Long userId, int year, int month) {
        MonthlyBudget budget = budgetRepository.findByUserIdAndMonthAndYear(userId, month, year)
                .orElse(null);

        if (budget == null) {
            // Return virtual zero-budget representation if none configured yet
            return computeVirtualBudget(userId, year, month);
        }

        return mapToDto(budget, userId);
    }

    @Transactional
    public MonthlyBudgetDto createBudget(Long userId, CreateBudgetRequest request) {
        if (budgetRepository.existsByUserIdAndMonthAndYear(userId, request.getMonth(), request.getYear())) {
            throw new DuplicateResourceException("Budget already exists for " +
                    Month.of(request.getMonth()).getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + request.getYear());
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        MonthlyBudget budget = MonthlyBudget.builder()
                .user(user)
                .month(request.getMonth())
                .year(request.getYear())
                .totalBudget(request.getTotalBudget())
                .warningThreshold(request.getWarningThreshold())
                .categoryBudgets(new ArrayList<>())
                .build();

        MonthlyBudget saved = budgetRepository.save(budget);

        if (request.getCategories() != null && !request.getCategories().isEmpty()) {
            for (CreateBudgetCategoryRequest catReq : request.getCategories()) {
                Category category = categoryRepository.findById(catReq.getCategoryId())
                        .orElseThrow(() -> new BadRequestException("Invalid category id: " + catReq.getCategoryId()));

                BudgetCategory budgetCategory = BudgetCategory.builder()
                        .monthlyBudget(saved)
                        .category(category)
                        .amount(catReq.getAmount())
                        .build();

                saved.getCategoryBudgets().add(budgetCategory);
            }
            saved = budgetRepository.save(saved);
        }

        // Check if alerts need to be fired
        checkAndCreateAlerts(userId, saved.getMonth(), saved.getYear());

        return mapToDto(saved, userId);
    }

    @Transactional
    public MonthlyBudgetDto updateBudget(Long userId, Long budgetId, UpdateBudgetRequest request) {
        MonthlyBudget budget = budgetRepository.findByIdAndUserId(budgetId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id: " + budgetId));

        budget.setTotalBudget(request.getTotalBudget());
        budget.setWarningThreshold(request.getWarningThreshold());

        // Update category budgets
        budget.getCategoryBudgets().clear();
        if (request.getCategories() != null && !request.getCategories().isEmpty()) {
            for (CreateBudgetCategoryRequest catReq : request.getCategories()) {
                Category category = categoryRepository.findById(catReq.getCategoryId())
                        .orElseThrow(() -> new BadRequestException("Invalid category id: " + catReq.getCategoryId()));

                BudgetCategory budgetCategory = BudgetCategory.builder()
                        .monthlyBudget(budget)
                        .category(category)
                        .amount(catReq.getAmount())
                        .build();

                budget.getCategoryBudgets().add(budgetCategory);
            }
        }

        MonthlyBudget updated = budgetRepository.save(budget);

        // Recompute alerts
        checkAndCreateAlerts(userId, updated.getMonth(), updated.getYear());

        return mapToDto(updated, userId);
    }

    @Transactional
    public void deleteBudget(Long userId, Long budgetId) {
        MonthlyBudget budget = budgetRepository.findByIdAndUserId(budgetId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id: " + budgetId));

        budgetRepository.delete(budget);
    }

    @Transactional
    public void checkAndCreateAlerts(Long userId, int month, int year) {
        Optional<MonthlyBudget> optBudget = budgetRepository.findByUserIdAndMonthAndYear(userId, month, year);
        if (optBudget.isEmpty()) return;

        User user = userRepository.findById(userId).orElse(null);
        if (user == null || !user.isBudgetNotificationsEnabled()) return;

        MonthlyBudget budget = optBudget.get();
        if (budget.getTotalBudget().compareTo(BigDecimal.ZERO) <= 0) return;

        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<Expense> expenses = expenseRepository.findByUserIdAndExpenseDateBetween(userId, start, end);
        BigDecimal totalSpent = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double percentage = totalSpent.divide(budget.getTotalBudget(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).doubleValue();

        String monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        String monthTag = monthName + " " + year;

        if (percentage >= 100.0) {
            boolean alreadyExceeded = notificationService.hasAlertInMonth(userId, NotificationType.BUDGET_EXCEEDED, monthTag);
            if (!alreadyExceeded) {
                String title = "Budget Exceeded!";
                String message = String.format("You have exceeded your %s budget! Spent ₹%.2f of ₹%.2f (%.1f%%).",
                        monthTag, totalSpent, budget.getTotalBudget(), percentage);
                notificationService.createNotification(userId, title, message, NotificationType.BUDGET_EXCEEDED);
            }
        } else if (percentage >= budget.getWarningThreshold()) {
            boolean alreadyWarned = notificationService.hasAlertInMonth(userId, NotificationType.BUDGET_WARNING, monthTag);
            if (!alreadyWarned) {
                String title = "Budget Warning Threshold Reached";
                String message = String.format("You have reached %.1f%% of your %s budget (Spent ₹%.2f of ₹%.2f).",
                        percentage, monthTag, totalSpent, budget.getTotalBudget());
                notificationService.createNotification(userId, title, message, NotificationType.BUDGET_WARNING);
            }
        }
    }

    private MonthlyBudgetDto mapToDto(MonthlyBudget budget, Long userId) {
        YearMonth ym = YearMonth.of(budget.getYear(), budget.getMonth());
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<Expense> expenses = expenseRepository.findByUserIdAndExpenseDateBetween(userId, start, end);

        BigDecimal totalSpent = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalBudget = budget.getTotalBudget();
        BigDecimal remaining = totalBudget.subtract(totalSpent);

        double percentage = 0.0;
        if (totalBudget.compareTo(BigDecimal.ZERO) > 0) {
            percentage = totalSpent.divide(totalBudget, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
        } else if (totalSpent.compareTo(BigDecimal.ZERO) > 0) {
            percentage = 100.0;
        }

        boolean isOverBudget = totalBudget.compareTo(BigDecimal.ZERO) > 0 && totalSpent.compareTo(totalBudget) > 0;
        boolean isWarning = totalBudget.compareTo(BigDecimal.ZERO) > 0 &&
                percentage >= budget.getWarningThreshold() && !isOverBudget;

        Map<Long, BigDecimal> catSpendMap = expenses.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getCategory().getId(),
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                ));

        List<BudgetCategoryDto> categoryDtos = new ArrayList<>();
        if (budget.getCategoryBudgets() != null) {
            for (BudgetCategory bc : budget.getCategoryBudgets()) {
                Category cat = bc.getCategory();
                BigDecimal catBudget = bc.getAmount();
                BigDecimal catSpent = catSpendMap.getOrDefault(cat.getId(), BigDecimal.ZERO);
                BigDecimal catRemaining = catBudget.subtract(catSpent);

                double catPercentage = 0.0;
                if (catBudget.compareTo(BigDecimal.ZERO) > 0) {
                    catPercentage = catSpent.divide(catBudget, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).doubleValue();
                } else if (catSpent.compareTo(BigDecimal.ZERO) > 0) {
                    catPercentage = 100.0;
                }

                categoryDtos.add(BudgetCategoryDto.builder()
                        .id(bc.getId())
                        .categoryId(cat.getId())
                        .categoryName(cat.getName())
                        .categoryIcon(cat.getIcon())
                        .categoryColor(cat.getColor())
                        .budgetAmount(catBudget)
                        .spentAmount(catSpent)
                        .remainingAmount(catRemaining)
                        .percentageUsed(catPercentage)
                        .build());
            }
        }

        return MonthlyBudgetDto.builder()
                .id(budget.getId())
                .month(budget.getMonth())
                .year(budget.getYear())
                .totalBudget(totalBudget)
                .warningThreshold(budget.getWarningThreshold())
                .spent(totalSpent)
                .remaining(remaining)
                .percentage(percentage)
                .isOverBudget(isOverBudget)
                .isWarning(isWarning)
                .categories(categoryDtos)
                .createdAt(budget.getCreatedAt())
                .updatedAt(budget.getUpdatedAt())
                .build();
    }

    private MonthlyBudgetDto computeVirtualBudget(Long userId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<Expense> expenses = expenseRepository.findByUserIdAndExpenseDateBetween(userId, start, end);
        BigDecimal totalSpent = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return MonthlyBudgetDto.builder()
                .id(null)
                .month(month)
                .year(year)
                .totalBudget(BigDecimal.ZERO)
                .warningThreshold(80)
                .spent(totalSpent)
                .remaining(BigDecimal.ZERO.subtract(totalSpent))
                .percentage(0.0)
                .isOverBudget(false)
                .isWarning(false)
                .categories(new ArrayList<>())
                .createdAt(null)
                .updatedAt(null)
                .build();
    }
}
