package com.expensetracker;

import com.expensetracker.budget.CreateBudgetCategoryRequest;
import com.expensetracker.budget.CreateBudgetRequest;
import com.expensetracker.expense.CreateExpenseRequest;
import com.expensetracker.expense.PaymentMethod;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class BudgetIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testZeroBudgetCalculationSafeHandling() throws Exception {
        String token = getDemoUserToken();

        // Query a future month that has no budget (e.g. Dec 2026) -> should return virtual zero-budget without error
        mockMvc.perform(get("/api/budgets/2026/12")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalBudget").value(0))
                .andExpect(jsonPath("$.data.percentage").value(0.0))
                .andExpect(jsonPath("$.data.isOverBudget").value(false));
    }

    @Test
    void testBudgetCreationAndAlertGeneration() throws Exception {
        String token = getDemoUserToken();

        // 1. Create a budget for November 2026 (month 11) with threshold 50% and total 1,000 INR
        CreateBudgetRequest request = CreateBudgetRequest.builder()
                .month(11)
                .year(2026)
                .totalBudget(new BigDecimal("1000.00"))
                .warningThreshold(50)
                .categories(List.of(
                        CreateBudgetCategoryRequest.builder()
                                .categoryId(1L)
                                .amount(new BigDecimal("500.00"))
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/budgets")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.month").value(11))
                .andExpect(jsonPath("$.data.totalBudget").value(1000.00));

        // 2. Add an expense of 600 INR in Nov 2026 -> should trigger BUDGET_WARNING (60% >= 50%)
        CreateExpenseRequest exp1 = CreateExpenseRequest.builder()
                .amount(new BigDecimal("600.00"))
                .description("November Lunch")
                .categoryId(1L)
                .expenseDate(LocalDate.of(2026, 11, 5))
                .paymentMethod(PaymentMethod.UPI)
                .build();

        mockMvc.perform(post("/api/expenses")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exp1)))
                .andExpect(status().isCreated());

        // 3. Verify notification exists with BUDGET_WARNING
        mockMvc.perform(get("/api/notifications")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.type == 'BUDGET_WARNING')]").isNotEmpty());

        // 4. Add another expense of 500 INR in Nov 2026 -> total 1100 INR > 1000 INR -> should trigger BUDGET_EXCEEDED
        CreateExpenseRequest exp2 = CreateExpenseRequest.builder()
                .amount(new BigDecimal("500.00"))
                .description("November Grocery")
                .categoryId(9L)
                .expenseDate(LocalDate.of(2026, 11, 6))
                .paymentMethod(PaymentMethod.UPI)
                .build();

        mockMvc.perform(post("/api/expenses")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exp2)))
                .andExpect(status().isCreated());

        // 5. Verify notification exists with BUDGET_EXCEEDED
        mockMvc.perform(get("/api/notifications")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.type == 'BUDGET_EXCEEDED')]").isNotEmpty());
    }
}
