package com.expensetracker;

import com.expensetracker.expense.CreateExpenseRequest;
import com.expensetracker.expense.PaymentMethod;
import com.expensetracker.expense.UpdateExpenseRequest;
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
public class ExpenseIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testExpenseCRUDAndFiltering() throws Exception {
        String token = getDemoUserToken();

        // 1. Create an expense
        CreateExpenseRequest createRequest = CreateExpenseRequest.builder()
                .amount(new BigDecimal("1250.50"))
                .description("Team Dinner at Olive Bistro")
                .categoryId(1L) // Food
                .expenseDate(LocalDate.of(2026, 10, 11))
                .paymentMethod(PaymentMethod.UPI)
                .notes("Split bill with work team")
                .build();

        String response = mockMvc.perform(post("/api/expenses")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.description").value("Team Dinner at Olive Bistro"))
                .andExpect(jsonPath("$.data.amount").value(1250.50))
                .andReturn().getResponse().getContentAsString();

        Number idNumber = com.jayway.jsonpath.JsonPath.read(response, "$.data.id");
        Long createdId = idNumber.longValue();

        // 2. Read single expense
        mockMvc.perform(get("/api/expenses/" + createdId)
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(createdId));

        // 3. Update expense
        UpdateExpenseRequest updateRequest = UpdateExpenseRequest.builder()
                .amount(new BigDecimal("1300.00"))
                .description("Updated Dinner")
                .categoryId(1L)
                .expenseDate(LocalDate.of(2026, 10, 11))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .notes("Updated notes")
                .build();

        mockMvc.perform(put("/api/expenses/" + createdId)
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount").value(1300.00))
                .andExpect(jsonPath("$.data.description").value("Updated Dinner"));

        // 4. Test filtering and search
        mockMvc.perform(get("/api/expenses")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .param("search", "Updated Dinner")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        // 5. Delete expense
        mockMvc.perform(delete("/api/expenses/" + createdId)
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());

        // 6. Verify deleted
        mockMvc.perform(get("/api/expenses/" + createdId)
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNotFound());
    }
}
