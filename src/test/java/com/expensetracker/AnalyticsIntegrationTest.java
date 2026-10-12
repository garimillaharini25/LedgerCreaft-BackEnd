package com.expensetracker;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AnalyticsIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testAnalyticsSummaryAndInsights() throws Exception {
        String token = getDemoUserToken();

        // 1. Get summary KPIs
        mockMvc.perform(get("/api/analytics/summary")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .param("month", "10")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalSpendingThisMonth").isNumber())
                .andExpect(jsonPath("$.data.transactionCount").isNumber());

        // 2. Get category breakdown
        mockMvc.perform(get("/api/analytics/category")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .param("month", "10")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        // 3. Get monthly comparison
        mockMvc.perform(get("/api/analytics/monthly")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        // 4. Get behavioral insights
        mockMvc.perform(get("/api/analytics/insights")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .param("month", "10")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }
}
