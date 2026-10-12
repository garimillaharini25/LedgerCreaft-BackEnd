package com.expensetracker;

import com.expensetracker.expense.UpdateExpenseRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UserIsolationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testUserIsolationForExpenses() throws Exception {
        String user1Token = getDemoUserToken();
        String user2Token = getUser2Token();

        // Expense 33 belongs to User 2
        // User 1 attempts to READ Expense 33 -> 404
        mockMvc.perform(get("/api/expenses/33")
                        .header(HttpHeaders.AUTHORIZATION, user1Token))
                .andExpect(status().isNotFound());

        // User 1 attempts to UPDATE Expense 33 -> 404
        UpdateExpenseRequest updateReq = UpdateExpenseRequest.builder()
                .amount(new BigDecimal("100.00"))
                .description("Hacked Description")
                .categoryId(10L)
                .expenseDate(LocalDate.now())
                .paymentMethod(PaymentMethod.CASH)
                .build();

        mockMvc.perform(put("/api/expenses/33")
                        .header(HttpHeaders.AUTHORIZATION, user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound());

        // User 1 attempts to DELETE Expense 33 -> 404
        mockMvc.perform(delete("/api/expenses/33")
                        .header(HttpHeaders.AUTHORIZATION, user1Token))
                .andExpect(status().isNotFound());

        // User 2 CAN read Expense 33
        mockMvc.perform(get("/api/expenses/33")
                        .header(HttpHeaders.AUTHORIZATION, user2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(33));

        // Expense 1 belongs to User 1
        // User 2 attempts to READ Expense 1 -> 404
        mockMvc.perform(get("/api/expenses/1")
                        .header(HttpHeaders.AUTHORIZATION, user2Token))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUserIsolationForBudgets() throws Exception {
        String user1Token = getDemoUserToken();

        // Budget 2 belongs to User 2
        // User 1 attempts to DELETE Budget 2 -> 404
        mockMvc.perform(delete("/api/budgets/2")
                        .header(HttpHeaders.AUTHORIZATION, user1Token))
                .andExpect(status().isNotFound());
    }
}
