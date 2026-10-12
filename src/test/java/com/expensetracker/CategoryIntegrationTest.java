package com.expensetracker;

import com.expensetracker.category.CreateCategoryRequest;
import com.expensetracker.category.UpdateCategoryRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CategoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testSystemCategoryProtection() throws Exception {
        String token = getDemoUserToken();

        // Try deleting Food (id = 1, system category) -> should return 403 Forbidden
        mockMvc.perform(delete("/api/categories/1")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isForbidden());

        // Try updating Food -> should return 403 Forbidden
        UpdateCategoryRequest updateReq = UpdateCategoryRequest.builder()
                .name("Food Modified")
                .icon("utensils")
                .color("#000000")
                .build();

        mockMvc.perform(put("/api/categories/1")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testCustomCategoryCreationAndSafeDeleteReassignment() throws Exception {
        String token = getDemoUserToken();

        // 1. Create custom category
        CreateCategoryRequest createReq = CreateCategoryRequest.builder()
                .name("Gym & Fitness")
                .description("Gym membership and proteins")
                .icon("activity")
                .color("#14b8a6")
                .build();

        String res = mockMvc.perform(post("/api/categories")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Gym & Fitness"))
                .andReturn().getResponse().getContentAsString();

        Number catIdNum = com.jayway.jsonpath.JsonPath.read(res, "$.data.id");
        Long customCatId = catIdNum.longValue();

        // 2. Create an expense under this custom category
        CreateExpenseRequest expReq = CreateExpenseRequest.builder()
                .amount(new BigDecimal("2500.00"))
                .description("Annual Gym Fee")
                .categoryId(customCatId)
                .expenseDate(LocalDate.of(2026, 10, 11))
                .paymentMethod(PaymentMethod.UPI)
                .build();

        String expRes = mockMvc.perform(post("/api/expenses")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Number expIdNum = com.jayway.jsonpath.JsonPath.read(expRes, "$.data.id");
        Long expId = expIdNum.longValue();

        // 3. Delete the custom category -> should safely reassign expense to "Other" (id = 12)
        mockMvc.perform(delete("/api/categories/" + customCatId)
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());

        // 4. Verify the expense still exists and its category is now "Other"
        mockMvc.perform(get("/api/expenses/" + expId)
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.category.name").value("Other"));
    }
}
