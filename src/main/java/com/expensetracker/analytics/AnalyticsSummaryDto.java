package com.expensetracker.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsSummaryDto {
    private BigDecimal totalSpendingThisMonth;
    private BigDecimal monthlyBudget;
    private BigDecimal remainingBudget;
    private double budgetPercentageUsed;
    private BigDecimal todaySpending;
    private BigDecimal thisWeekSpending;
    private long transactionCount;
    private BigDecimal largestExpense;
    private String largestExpenseDescription;
    private BigDecimal averageDailySpend;
}
