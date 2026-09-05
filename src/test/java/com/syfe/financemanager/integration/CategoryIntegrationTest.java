package com.syfe.financemanager.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import java.util.Map;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CategoryIntegrationTest extends AbstractIntegrationTest {

    @Test
    void getCategories_withoutSession_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/categories")).andExpect(status().isUnauthorized());
    }

    @Test
    void getCategories_returnsSeededDefaults() throws Exception {
        MockHttpSession session = registerAndLogin("cat-defaults@example.com");

        mockMvc.perform(get("/api/categories").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories", org.hamcrest.Matchers.hasSize(7)))
                .andExpect(jsonPath("$.categories[*].name", hasItem("Salary")))
                .andExpect(jsonPath("$.categories[*].name", hasItem("Food")));
    }

    @Test
    void createCustomCategory_thenDuplicateForSameUser_returnsConflict() throws Exception {
        MockHttpSession session = registerAndLogin("cat-dup@example.com");
        Map<String, String> body = Map.of("name", "SideBusinessIncome", "type", "INCOME");

        mockMvc.perform(post("/api/categories")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isCustom").value(true))
                .andExpect(jsonPath("$.custom").value(true));

        mockMvc.perform(post("/api/categories")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict());
    }

    @Test
    void sameCategoryName_allowedForDifferentUsers() throws Exception {
        MockHttpSession sessionA = registerAndLogin("cat-userA@example.com");
        MockHttpSession sessionB = registerAndLogin("cat-userB@example.com");
        Map<String, String> body = Map.of("name", "SharedName", "type", "EXPENSE");

        mockMvc.perform(post("/api/categories").session(sessionA)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/categories").session(sessionB)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }

    @Test
    void deleteDefaultCategory_returnsBadRequest() throws Exception {
        MockHttpSession session = registerAndLogin("cat-deldefault@example.com");

        mockMvc.perform(delete("/api/categories/Salary").session(session))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteNonexistentCategory_returnsNotFound() throws Exception {
        MockHttpSession session = registerAndLogin("cat-delghost@example.com");

        mockMvc.perform(delete("/api/categories/GhostCategory").session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteAnotherUsersCustomCategory_returnsForbidden() throws Exception {
        MockHttpSession sessionA = registerAndLogin("cat-forbid-owner@example.com");
        MockHttpSession sessionB = registerAndLogin("cat-forbid-other@example.com");
        Map<String, String> body = Map.of("name", "OwnerOnlyCategory", "type", "EXPENSE");

        mockMvc.perform(post("/api/categories").session(sessionA)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/categories/OwnerOnlyCategory").session(sessionB))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteCategoryReferencedByTransaction_returnsConflict() throws Exception {
        MockHttpSession session = registerAndLogin("cat-referenced@example.com");
        Map<String, String> categoryBody = Map.of("name", "ReferencedCategory", "type", "EXPENSE");

        mockMvc.perform(post("/api/categories").session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(categoryBody)))
                .andExpect(status().isCreated());

        Map<String, Object> txnBody = Map.of("amount", "20.00", "date", "2024-01-01", "category", "ReferencedCategory");
        mockMvc.perform(post("/api/transactions").session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(txnBody)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/categories/ReferencedCategory").session(session))
                .andExpect(status().isConflict());
    }
}
