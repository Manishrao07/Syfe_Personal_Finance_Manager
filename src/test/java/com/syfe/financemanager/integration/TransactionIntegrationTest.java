package com.syfe.financemanager.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import java.time.LocalDate;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionIntegrationTest extends AbstractIntegrationTest {

    private Long createTransaction(MockHttpSession session, String category, String amount, String date) throws Exception {
        Map<String, Object> body = Map.of("amount", amount, "date", date, "category", category, "description", "test");
        String response = mockMvc.perform(post("/api/transactions")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    @Test
    void create_withoutSession_returnsUnauthorized() throws Exception {
        Map<String, Object> body = Map.of("amount", "10.00", "date", "2024-01-01", "category", "Salary");
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createReadUpdateDelete_fullFlow() throws Exception {
        MockHttpSession session = registerAndLogin("txn-flow@example.com");

        Long id = createTransaction(session, "Salary", "50000.00", "2024-01-15");

        mockMvc.perform(get("/api/transactions").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions[0].id").value(id))
                .andExpect(jsonPath("$.transactions[0].type").value("INCOME"));

        Map<String, Object> updateBody = Map.of("amount", "60000.00", "description", "Updated", "date", "2099-01-01");
        mockMvc.perform(put("/api/transactions/" + id)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(60000.00))
                .andExpect(jsonPath("$.description").value("Updated"))
                .andExpect(jsonPath("$.date").value("2024-01-15"));

        mockMvc.perform(delete("/api/transactions/" + id).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Transaction deleted successfully"));

        mockMvc.perform(get("/api/transactions").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions").isEmpty());

        mockMvc.perform(put("/api/transactions/" + id)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("amount", "1.00"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_withFutureDate_returnsBadRequest() throws Exception {
        MockHttpSession session = registerAndLogin("txn-future@example.com");
        Map<String, Object> body = Map.of("amount", "10.00", "date", LocalDate.now().plusDays(1).toString(), "category", "Salary");

        mockMvc.perform(post("/api/transactions")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withInvalidCategory_returnsBadRequest() throws Exception {
        MockHttpSession session = registerAndLogin("txn-badcat@example.com");
        Map<String, Object> body = Map.of("amount", "10.00", "date", "2024-01-01", "category", "DoesNotExist");

        mockMvc.perform(post("/api/transactions")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withNegativeAmount_returnsBadRequest() throws Exception {
        MockHttpSession session = registerAndLogin("txn-negamount@example.com");
        Map<String, Object> body = Map.of("amount", "-10.00", "date", "2024-01-01", "category", "Salary");

        mockMvc.perform(post("/api/transactions")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateOrDelete_anotherUsersTransaction_returnsNotFound() throws Exception {
        MockHttpSession ownerSession = registerAndLogin("txn-owner@example.com");
        Long id = createTransaction(ownerSession, "Salary", "100.00", "2024-01-01");

        MockHttpSession otherSession = registerAndLogin("txn-other@example.com");

        mockMvc.perform(put("/api/transactions/" + id)
                        .session(otherSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("amount", "1.00"))))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/transactions/" + id).session(otherSession))
                .andExpect(status().isNotFound());
    }
}
