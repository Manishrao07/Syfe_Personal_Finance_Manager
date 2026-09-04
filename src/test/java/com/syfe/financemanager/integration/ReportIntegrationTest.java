package com.syfe.financemanager.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportIntegrationTest extends AbstractIntegrationTest {

    @Test
    void monthlyAndYearlyReports_reflectRecordedTransactions() throws Exception {
        MockHttpSession session = registerAndLogin("report-flow@example.com");

        createTransaction(session, "Salary", "3000.00", "2024-01-10");
        createTransaction(session, "Food", "400.00", "2024-01-12");
        createTransaction(session, "Rent", "1200.00", "2024-01-01");

        mockMvc.perform(get("/api/reports/monthly/2024/1").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value(1))
                .andExpect(jsonPath("$.year").value(2024))
                .andExpect(jsonPath("$.totalIncome.Salary").value(3000.00))
                .andExpect(jsonPath("$.totalExpenses.Food").value(400.00))
                .andExpect(jsonPath("$.totalExpenses.Rent").value(1200.00))
                .andExpect(jsonPath("$.netSavings").value(1400.00))
                .andExpect(jsonPath("$.totalExpenses.Healthcare").doesNotExist());

        mockMvc.perform(get("/api/reports/yearly/2024").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2024))
                .andExpect(jsonPath("$.totalIncome.Salary").value(3000.00))
                .andExpect(jsonPath("$.netSavings").value(1400.00));
    }

    @Test
    void monthlyReport_withInvalidMonth_returnsBadRequest() throws Exception {
        MockHttpSession session = registerAndLogin("report-badmonth@example.com");

        mockMvc.perform(get("/api/reports/monthly/2024/13").session(session))
                .andExpect(status().isBadRequest());
    }

    private void createTransaction(MockHttpSession session, String category, String amount, String date) throws Exception {
        Map<String, Object> body = Map.of("amount", amount, "date", date, "category", category);
        mockMvc.perform(post("/api/transactions")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }
}
