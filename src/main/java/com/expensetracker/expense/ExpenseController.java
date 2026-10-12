package com.expensetracker.expense;

import com.expensetracker.common.ApiResponse;
import com.expensetracker.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@Tag(name = "Expense Management", description = "Endpoints for managing transactions with rich filters and sorting")
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping
    @Operation(summary = "Get paginated, filtered and sorted expenses for authenticated user")
    public ResponseEntity<ApiResponse<ExpensePageResponse>> getExpenses(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "category", required = false) Long categoryId,
            @RequestParam(value = "paymentMethod", required = false) PaymentMethod paymentMethod,
            @RequestParam(value = "minAmount", required = false) BigDecimal minAmount,
            @RequestParam(value = "maxAmount", required = false) BigDecimal maxAmount,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sort", defaultValue = "newest") String sort) {

        ExpensePageResponse response = expenseService.getExpenses(
                principal.getId(), startDate, endDate, categoryId, paymentMethod, minAmount, maxAmount, search, page, size, sort);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get single expense detail by ID")
    public ResponseEntity<ApiResponse<ExpenseDto>> getExpenseById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") Long id) {
        ExpenseDto expense = expenseService.getExpenseById(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(expense));
    }

    @PostMapping
    @Operation(summary = "Create a new expense entry")
    public ResponseEntity<ApiResponse<ExpenseDto>> createExpense(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateExpenseRequest request) {
        ExpenseDto expense = expenseService.createExpense(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Expense added successfully", expense));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing expense entry")
    public ResponseEntity<ApiResponse<ExpenseDto>> updateExpense(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateExpenseRequest request) {
        ExpenseDto expense = expenseService.updateExpense(principal.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Expense updated successfully", expense));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an expense entry")
    public ResponseEntity<ApiResponse<Void>> deleteExpense(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") Long id) {
        expenseService.deleteExpense(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Expense deleted successfully", null));
    }
}
