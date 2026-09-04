package com.syfe.financemanager.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import java.time.LocalDate;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GoalIntegrationTest extends AbstractIntegrationTest {

    @Test
    void createReadUpdateDelete_fullFlow() throws Exception {
        MockHttpSession session = registerAndLogin("goal-flow@example.com");

        Map<String, Object> createBody = Map.of(
                "goalName", "Emergency Fund",
                "targetAmount", "5000.00",
                "targetDate", LocalDate.now().plusYears(1).toString());

        String created = mockMvc.perform(post("/api/goals")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.goalName").value("Emergency Fund"))
                .andExpect(jsonPath("$.currentProgress").value(0.0))
                .andExpect(jsonPath("$.progressPercentage").value(0.0))
                .andReturn().getResponse().getContentAsString();

        long id = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(get("/api/goals").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goals[0].id").value(id));

        mockMvc.perform(get("/api/goals/" + id).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        Map<String, Object> updateBody = Map.of("targetAmount", "6000.00");
        mockMvc.perform(put("/api/goals/" + id)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetAmount").value(6000.00));

        mockMvc.perform(delete("/api/goals/" + id).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Goal deleted successfully"));

        mockMvc.perform(get("/api/goals/" + id).session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_withPastTargetDate_returnsBadRequest() throws Exception {
        MockHttpSession session = registerAndLogin("goal-pastdate@example.com");
        Map<String, Object> body = Map.of("goalName", "Bad Goal", "targetAmount", "100.00",
                "targetDate", LocalDate.now().minusDays(1).toString());

        mockMvc.perform(post("/api/goals")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void accessingAnotherUsersGoal_returnsForbidden() throws Exception {
        MockHttpSession ownerSession = registerAndLogin("goal-owner@example.com");
        Map<String, Object> body = Map.of("goalName", "Owner Goal", "targetAmount", "100.00",
                "targetDate", LocalDate.now().plusMonths(1).toString());

        String created = mockMvc.perform(post("/api/goals")
                        .session(ownerSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(created).get("id").asLong();

        MockHttpSession otherSession = registerAndLogin("goal-intruder@example.com");

        mockMvc.perform(get("/api/goals/" + id).session(otherSession))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/goals/" + id).session(otherSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("targetAmount", "1.00"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/goals/" + id).session(otherSession))
                .andExpect(status().isForbidden());
    }
}
