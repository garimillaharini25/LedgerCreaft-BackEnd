package com.expensetracker.budget;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyBudgetDto {
    private Long id;
    private int month;
    private int year;
    private BigDecimal totalBudget;
    private int warningThreshold;
    private BigDecimal spent;
    private BigDecimal remaining;
    private double percentage;
    @com.fasterxml.jackson.annotation.JsonProperty("isOverBudget")
    private boolean isOverBudget;
    @com.fasterxml.jackson.annotation.JsonProperty("isWarning")
    private boolean isWarning;
    private List<BudgetCategoryDto> categories;
    private Instant createdAt;
    private Instant updatedAt;
}
