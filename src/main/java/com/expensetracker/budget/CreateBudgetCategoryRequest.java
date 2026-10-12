package com.expensetracker.budget;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBudgetCategoryRequest {

    @NotNull(message = "Category id is required")
    private Long categoryId;

    @NotNull(message = "Category budget amount is required")
    @DecimalMin(value = "0.00", message = "Category budget amount must be at least 0")
    private BigDecimal amount;
}
