package com.expensetracker.budget;

import com.expensetracker.common.ApiResponse;
import com.expensetracker.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
@Tag(name = "Budget Management", description = "Endpoints for monthly budgets and category threshold tracking")
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping
    @Operation(summary = "Get all historical monthly budgets for user")
    public ResponseEntity<ApiResponse<List<MonthlyBudgetDto>>> getAllBudgets(@AuthenticationPrincipal UserPrincipal principal) {
        List<MonthlyBudgetDto> budgets = budgetService.getAllBudgets(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(budgets));
    }

    @GetMapping("/{year}/{month}")
    @Operation(summary = "Get monthly budget details and real spending for specific month and year")
    public ResponseEntity<ApiResponse<MonthlyBudgetDto>> getBudgetByYearAndMonth(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("year") int year,
            @PathVariable("month") int month) {
        MonthlyBudgetDto budget = budgetService.getBudgetByYearAndMonth(principal.getId(), year, month);
        return ResponseEntity.ok(ApiResponse.success(budget));
    }

    @PostMapping
    @Operation(summary = "Create a new monthly budget with optional category breakdowns")
    public ResponseEntity<ApiResponse<MonthlyBudgetDto>> createBudget(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateBudgetRequest request) {
        MonthlyBudgetDto budget = budgetService.createBudget(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Monthly budget created successfully", budget));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing monthly budget and category limits")
    public ResponseEntity<ApiResponse<MonthlyBudgetDto>> updateBudget(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateBudgetRequest request) {
        MonthlyBudgetDto budget = budgetService.updateBudget(principal.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Monthly budget updated successfully", budget));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a monthly budget")
    public ResponseEntity<ApiResponse<Void>> deleteBudget(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") Long id) {
        budgetService.deleteBudget(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Monthly budget deleted successfully", null));
    }
}
