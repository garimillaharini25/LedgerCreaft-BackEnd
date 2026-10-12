package com.expensetracker.analytics;

import com.expensetracker.common.ApiResponse;
import com.expensetracker.expense.ExpenseDto;
import com.expensetracker.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics & Reports", description = "Endpoints for financial aggregations, charts and spending insights")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/summary")
    @Operation(summary = "Get high-level summary KPIs (total spend, budget %, today, this week, largest)")
    public ResponseEntity<ApiResponse<AnalyticsSummaryDto>> getSummary(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "year", required = false) Integer year) {
        AnalyticsSummaryDto summary = analyticsService.getSummary(principal.getId(), month, year);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/category")
    @Operation(summary = "Get category breakdown with percentages and counts")
    public ResponseEntity<ApiResponse<List<CategorySpendingDto>>> getCategoryBreakdown(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "year", required = false) Integer year) {
        List<CategorySpendingDto> breakdown = analyticsService.getCategoryBreakdown(principal.getId(), startDate, endDate, month, year);
        return ResponseEntity.ok(ApiResponse.success(breakdown));
    }

    @GetMapping("/monthly")
    @Operation(summary = "Get historical monthly comparison of spending versus budget")
    public ResponseEntity<ApiResponse<List<MonthlyComparisonDto>>> getMonthlyComparison(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(value = "months", defaultValue = "6") int months) {
        List<MonthlyComparisonDto> comparison = analyticsService.getMonthlyComparison(principal.getId(), months);
        return ResponseEntity.ok(ApiResponse.success(comparison));
    }

    @GetMapping("/payment-methods")
    @Operation(summary = "Get breakdown by payment methods")
    public ResponseEntity<ApiResponse<List<PaymentMethodSpendingDto>>> getPaymentMethods(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "year", required = false) Integer year) {
        List<PaymentMethodSpendingDto> breakdown = analyticsService.getPaymentMethodsBreakdown(principal.getId(), startDate, endDate, month, year);
        return ResponseEntity.ok(ApiResponse.success(breakdown));
    }

    @GetMapping("/trend")
    @Operation(summary = "Get day-by-day spending trend")
    public ResponseEntity<ApiResponse<List<DailyTrendDto>>> getTrend(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "year", required = false) Integer year) {
        List<DailyTrendDto> trend = analyticsService.getDailyTrend(principal.getId(), startDate, endDate, month, year);
        return ResponseEntity.ok(ApiResponse.success(trend));
    }

    @GetMapping("/top-expenses")
    @Operation(summary = "Get top highest expenses in selected period")
    public ResponseEntity<ApiResponse<List<ExpenseDto>>> getTopExpenses(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(value = "limit", defaultValue = "5") int limit,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "year", required = false) Integer year) {
        List<ExpenseDto> topExpenses = analyticsService.getTopExpenses(principal.getId(), limit, startDate, endDate, month, year);
        return ResponseEntity.ok(ApiResponse.success(topExpenses));
    }

    @GetMapping("/insights")
    @Operation(summary = "Get intelligent behavioral insights from actual expense and budget data")
    public ResponseEntity<ApiResponse<List<InsightDto>>> getInsights(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "year", required = false) Integer year) {
        List<InsightDto> insights = analyticsService.generateInsights(principal.getId(), month, year);
        return ResponseEntity.ok(ApiResponse.success(insights));
    }
}
