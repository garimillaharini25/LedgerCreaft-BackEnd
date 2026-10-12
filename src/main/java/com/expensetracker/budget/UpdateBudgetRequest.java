package com.expensetracker.budget;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBudgetRequest {

    @NotNull(message = "Total budget is required")
    @DecimalMin(value = "0.00", message = "Total budget must be at least 0")
    private BigDecimal totalBudget;

    @NotNull(message = "Warning threshold is required")
    @Min(value = 1, message = "Warning threshold must be between 1 and 100")
    @Max(value = 100, message = "Warning threshold must be between 1 and 100")
    private Integer warningThreshold;

    @Valid
    private List<CreateBudgetCategoryRequest> categories;
}
