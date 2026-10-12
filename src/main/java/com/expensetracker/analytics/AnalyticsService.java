package com.expensetracker.analytics;

import com.expensetracker.budget.MonthlyBudget;
import com.expensetracker.budget.MonthlyBudgetRepository;
import com.expensetracker.category.Category;
import com.expensetracker.expense.Expense;
import com.expensetracker.expense.ExpenseDto;
import com.expensetracker.expense.ExpenseRepository;
import com.expensetracker.expense.PaymentMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ExpenseRepository expenseRepository;
    private final MonthlyBudgetRepository budgetRepository;

    @Transactional(readOnly = true)
    public AnalyticsSummaryDto getSummary(Long userId, Integer month, Integer year) {
        LocalDate today = LocalDate.now();
        int targetMonth = (month != null) ? month : today.getMonthValue();
        int targetYear = (year != null) ? year : today.getYear();

        YearMonth ym = YearMonth.of(targetYear, targetMonth);
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();

        List<Expense> monthExpenses = expenseRepository.findByUserIdAndExpenseDateBetween(userId, monthStart, monthEnd);

        BigDecimal totalMonthSpend = monthExpenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Today spending
        BigDecimal todaySpend = monthExpenses.stream()
                .filter(e -> e.getExpenseDate().equals(today))
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // This week spending
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        BigDecimal thisWeekSpend = expenseRepository.findByUserIdAndExpenseDateBetween(userId, weekStart, weekEnd).stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Budget info
        MonthlyBudget budget = budgetRepository.findByUserIdAndMonthAndYear(userId, targetMonth, targetYear).orElse(null);
        BigDecimal budgetAmount = (budget != null) ? budget.getTotalBudget() : BigDecimal.ZERO;
        BigDecimal remainingBudget = budgetAmount.subtract(totalMonthSpend);

        double percentageUsed = 0.0;
        if (budgetAmount.compareTo(BigDecimal.ZERO) > 0) {
            percentageUsed = totalMonthSpend.divide(budgetAmount, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
        } else if (totalMonthSpend.compareTo(BigDecimal.ZERO) > 0) {
            percentageUsed = 100.0;
        }

        // Largest expense
        Expense largestExp = monthExpenses.stream()
                .max(Comparator.comparing(Expense::getAmount))
                .orElse(null);

        BigDecimal largestAmount = (largestExp != null) ? largestExp.getAmount() : BigDecimal.ZERO;
        String largestDesc = (largestExp != null) ? largestExp.getDescription() : "None";

        // Average daily spend
        int daysDivisor = Math.max(1, (targetYear == today.getYear() && targetMonth == today.getMonthValue()) ? today.getDayOfMonth() : ym.lengthOfMonth());
        BigDecimal avgDailySpend = totalMonthSpend.divide(BigDecimal.valueOf(daysDivisor), 2, RoundingMode.HALF_UP);

        return AnalyticsSummaryDto.builder()
                .totalSpendingThisMonth(totalMonthSpend)
                .monthlyBudget(budgetAmount)
                .remainingBudget(remainingBudget)
                .budgetPercentageUsed(percentageUsed)
                .todaySpending(todaySpend)
                .thisWeekSpending(thisWeekSpend)
                .transactionCount(monthExpenses.size())
                .largestExpense(largestAmount)
                .largestExpenseDescription(largestDesc)
                .averageDailySpend(avgDailySpend)
                .build();
    }

    @Transactional(readOnly = true)
    public List<CategorySpendingDto> getCategoryBreakdown(Long userId, LocalDate startDate, LocalDate endDate, Integer month, Integer year) {
        List<Expense> expenses = getFilteredExpenses(userId, startDate, endDate, month, year);

        BigDecimal totalSpend = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<Category, List<Expense>> grouped = expenses.stream()
                .collect(Collectors.groupingBy(Expense::getCategory));

        List<CategorySpendingDto> result = new ArrayList<>();
        for (Map.Entry<Category, List<Expense>> entry : grouped.entrySet()) {
            Category cat = entry.getKey();
            List<Expense> catExpenses = entry.getValue();

            BigDecimal catTotal = catExpenses.stream()
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            double percentage = 0.0;
            if (totalSpend.compareTo(BigDecimal.ZERO) > 0) {
                percentage = catTotal.divide(totalSpend, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue();
            }

            result.add(CategorySpendingDto.builder()
                    .categoryId(cat.getId())
                    .categoryName(cat.getName())
                    .categoryColor(cat.getColor())
                    .categoryIcon(cat.getIcon())
                    .totalAmount(catTotal)
                    .percentage(percentage)
                    .transactionCount(catExpenses.size())
                    .build());
        }

        result.sort(Comparator.comparing(CategorySpendingDto::getTotalAmount).reversed());
        return result;
    }

    @Transactional(readOnly = true)
    public List<MonthlyComparisonDto> getMonthlyComparison(Long userId, int monthsCount) {
        int count = Math.max(3, Math.min(monthsCount, 12));
        LocalDate now = LocalDate.now();
        List<MonthlyComparisonDto> list = new ArrayList<>();

        for (int i = count - 1; i >= 0; i--) {
            YearMonth ym = YearMonth.from(now.minusMonths(i));
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();

            BigDecimal totalSpent = expenseRepository.findByUserIdAndExpenseDateBetween(userId, start, end).stream()
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            MonthlyBudget budget = budgetRepository.findByUserIdAndMonthAndYear(userId, ym.getMonthValue(), ym.getYear()).orElse(null);
            BigDecimal budgetAmount = (budget != null) ? budget.getTotalBudget() : BigDecimal.ZERO;

            String monthName = ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);

            list.add(MonthlyComparisonDto.builder()
                    .year(ym.getYear())
                    .month(ym.getMonthValue())
                    .monthName(monthName + " " + (ym.getYear() % 100))
                    .totalSpent(totalSpent)
                    .budgetAmount(budgetAmount)
                    .build());
        }

        return list;
    }

    @Transactional(readOnly = true)
    public List<PaymentMethodSpendingDto> getPaymentMethodsBreakdown(Long userId, LocalDate startDate, LocalDate endDate, Integer month, Integer year) {
        List<Expense> expenses = getFilteredExpenses(userId, startDate, endDate, month, year);

        BigDecimal total = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<PaymentMethod, List<Expense>> grouped = expenses.stream()
                .collect(Collectors.groupingBy(Expense::getPaymentMethod));

        List<PaymentMethodSpendingDto> list = new ArrayList<>();
        for (PaymentMethod pm : PaymentMethod.values()) {
            List<Expense> pmExpenses = grouped.getOrDefault(pm, Collections.emptyList());
            BigDecimal pmTotal = pmExpenses.stream()
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (pmTotal.compareTo(BigDecimal.ZERO) > 0 || !pmExpenses.isEmpty()) {
                double pct = 0.0;
                if (total.compareTo(BigDecimal.ZERO) > 0) {
                    pct = pmTotal.divide(total, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue();
                }

                String display = formatPaymentMethodName(pm);
                list.add(PaymentMethodSpendingDto.builder()
                        .paymentMethod(pm)
                        .displayName(display)
                        .totalAmount(pmTotal)
                        .percentage(pct)
                        .transactionCount(pmExpenses.size())
                        .build());
            }
        }

        list.sort(Comparator.comparing(PaymentMethodSpendingDto::getTotalAmount).reversed());
        return list;
    }

    @Transactional(readOnly = true)
    public List<DailyTrendDto> getDailyTrend(Long userId, LocalDate startDate, LocalDate endDate, Integer month, Integer year) {
        List<Expense> expenses = getFilteredExpenses(userId, startDate, endDate, month, year);

        Map<LocalDate, List<Expense>> grouped = expenses.stream()
                .collect(Collectors.groupingBy(Expense::getExpenseDate));

        return grouped.entrySet().stream()
                .map(entry -> DailyTrendDto.builder()
                        .date(entry.getKey())
                        .amount(entry.getValue().stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
                        .count(entry.getValue().size())
                        .build())
                .sorted(Comparator.comparing(DailyTrendDto::getDate))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ExpenseDto> getTopExpenses(Long userId, int limit, LocalDate startDate, LocalDate endDate, Integer month, Integer year) {
        int max = Math.max(1, Math.min(limit, 50));
        List<Expense> expenses = getFilteredExpenses(userId, startDate, endDate, month, year);

        return expenses.stream()
                .sorted(Comparator.comparing(Expense::getAmount).reversed())
                .limit(max)
                .map(ExpenseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InsightDto> generateInsights(Long userId, Integer month, Integer year) {
        LocalDate today = LocalDate.now();
        int targetMonth = (month != null) ? month : today.getMonthValue();
        int targetYear = (year != null) ? year : today.getYear();

        YearMonth currentYm = YearMonth.of(targetYear, targetMonth);
        YearMonth prevYm = currentYm.minusMonths(1);

        List<Expense> currentExpenses = expenseRepository.findByUserIdAndExpenseDateBetween(
                userId, currentYm.atDay(1), currentYm.atEndOfMonth());
        List<Expense> prevExpenses = expenseRepository.findByUserIdAndExpenseDateBetween(
                userId, prevYm.atDay(1), prevYm.atEndOfMonth());

        BigDecimal currentTotal = currentExpenses.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal prevTotal = prevExpenses.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<InsightDto> insights = new ArrayList<>();

        // 1. Budget usage insight
        MonthlyBudget budget = budgetRepository.findByUserIdAndMonthAndYear(userId, targetMonth, targetYear).orElse(null);
        if (budget != null && budget.getTotalBudget().compareTo(BigDecimal.ZERO) > 0) {
            double pct = currentTotal.divide(budget.getTotalBudget(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
            if (pct >= 100.0) {
                insights.add(InsightDto.builder()
                        .id("budget-exceeded")
                        .title("Monthly Budget Exceeded")
                        .description(String.format("You have used %.1f%% of your monthly budget (₹%.2f of ₹%.2f). Consider pausing non-essential expenses.",
                                pct, currentTotal, budget.getTotalBudget()))
                        .type("WARNING")
                        .icon("alert-triangle")
                        .build());
            } else if (pct >= budget.getWarningThreshold()) {
                insights.add(InsightDto.builder()
                        .id("budget-warning")
                        .title("Approaching Budget Limit")
                        .description(String.format("You have used %.1f%% of your ₹%.2f monthly budget. You have ₹%.2f remaining.",
                                pct, budget.getTotalBudget(), budget.getTotalBudget().subtract(currentTotal)))
                        .type("WARNING")
                        .icon("alert-circle")
                        .build());
            } else {
                insights.add(InsightDto.builder()
                        .id("budget-healthy")
                        .title("On Track with Budget")
                        .description(String.format("You have comfortably used %.1f%% of your monthly budget (₹%.2f remaining).",
                                pct, budget.getTotalBudget().subtract(currentTotal)))
                        .type("POSITIVE")
                        .icon("check-circle")
                        .build());
            }
        }

        // 2. Month-over-month category insight
        Map<String, BigDecimal> currentCatMap = currentExpenses.stream()
                .collect(Collectors.groupingBy(e -> e.getCategory().getName(),
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)));
        Map<String, BigDecimal> prevCatMap = prevExpenses.stream()
                .collect(Collectors.groupingBy(e -> e.getCategory().getName(),
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)));

        if (!currentCatMap.isEmpty()) {
            Map.Entry<String, BigDecimal> topCategory = currentCatMap.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .orElse(null);

            if (topCategory != null) {
                String catName = topCategory.getKey();
                BigDecimal currentCatAmount = topCategory.getValue();
                BigDecimal prevCatAmount = prevCatMap.getOrDefault(catName, BigDecimal.ZERO);

                if (prevCatAmount.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal diff = currentCatAmount.subtract(prevCatAmount);
                    double catPctDiff = diff.divide(prevCatAmount, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).doubleValue();

                    if (catPctDiff > 0) {
                        insights.add(InsightDto.builder()
                                .id("cat-trend-" + catName)
                                .title(catName + " Spending Increased")
                                .description(String.format("You spent %.1f%% more on %s than last month (₹%.2f vs ₹%.2f).",
                                        catPctDiff, catName, currentCatAmount, prevCatAmount))
                                .type("INFO")
                                .icon("trending-up")
                                .build());
                    } else if (catPctDiff < 0) {
                        insights.add(InsightDto.builder()
                                .id("cat-trend-" + catName)
                                .title(catName + " Savings")
                                .description(String.format("You spent %.1f%% less on %s compared to last month (saved ₹%.2f).",
                                        Math.abs(catPctDiff), catName, prevCatAmount.subtract(currentCatAmount)))
                                .type("POSITIVE")
                                .icon("trending-down")
                                .build());
                    }
                }
            }
        }

        // 3. Average daily spending rate
        int days = Math.max(1, (targetYear == today.getYear() && targetMonth == today.getMonthValue()) ? today.getDayOfMonth() : currentYm.lengthOfMonth());
        BigDecimal avgDaily = currentTotal.divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP);
        insights.add(InsightDto.builder()
                .id("daily-rate")
                .title("Daily Spending Rate")
                .description(String.format("Your average daily spending is ₹%.2f this month across %d active transactions.",
                        avgDaily, currentExpenses.size()))
                .type("INFO")
                .icon("clock")
                .build());

        // 4. Payment preference
        if (!currentExpenses.isEmpty()) {
            Map<PaymentMethod, Long> pmCount = currentExpenses.stream()
                    .collect(Collectors.groupingBy(Expense::getPaymentMethod, Collectors.counting()));
            Map.Entry<PaymentMethod, Long> topPm = pmCount.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .orElse(null);

            if (topPm != null) {
                double pmShare = (topPm.getValue() * 100.0) / currentExpenses.size();
                insights.add(InsightDto.builder()
                        .id("payment-method")
                        .title("Primary Payment Method")
                        .description(String.format("%s is your most used payment mode, making up %.0f%% of all transactions.",
                                formatPaymentMethodName(topPm.getKey()), pmShare))
                        .type("INFO")
                        .icon("credit-card")
                        .build());
            }
        }

        return insights;
    }

    private List<Expense> getFilteredExpenses(Long userId, LocalDate startDate, LocalDate endDate, Integer month, Integer year) {
        LocalDate start = startDate;
        LocalDate end = endDate;

        if (start == null && end == null) {
            if (month != null && year != null) {
                YearMonth ym = YearMonth.of(year, month);
                start = ym.atDay(1);
                end = ym.atEndOfMonth();
            } else if (year != null) {
                start = LocalDate.of(year, 1, 1);
                end = LocalDate.of(year, 12, 31);
            } else {
                // Default to all user expenses
                return expenseRepository.findByUserId(userId);
            }
        }

        if (start != null && end != null) {
            return expenseRepository.findByUserIdAndExpenseDateBetween(userId, start, end);
        } else if (start != null) {
            return expenseRepository.findByUserIdAndExpenseDateBetween(userId, start, LocalDate.of(2100, 12, 31));
        } else {
            return expenseRepository.findByUserIdAndExpenseDateBetween(userId, LocalDate.of(2000, 1, 1), end);
        }
    }

    private String formatPaymentMethodName(PaymentMethod pm) {
        return switch (pm) {
            case UPI -> "UPI";
            case CREDIT_CARD -> "Credit Card";
            case DEBIT_CARD -> "Debit Card";
            case BANK_TRANSFER -> "Bank Transfer";
            case CASH -> "Cash";
            case OTHER -> "Other";
        };
    }
}
