package com.syfe.financemanager.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/** Response body for {@code GET /api/goals}. */
@Getter
@AllArgsConstructor
public class GoalListResponse {
    private final List<GoalResponse> goals;
}
