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
public class MonthlyComparisonDto {
    private int year;
    private int month;
    private String monthName;
    private BigDecimal totalSpent;
    private BigDecimal budgetAmount;
}
