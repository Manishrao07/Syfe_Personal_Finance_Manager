package com.syfe.financemanager.dto.request;

import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Payload for {@code PUT /api/goals/{id}}. Only target amount/date may change. */
@Getter
@Setter
public class GoalUpdateRequest {

    @Positive(message = "targetAmount must be positive")
    private BigDecimal targetAmount;

    private LocalDate targetDate;
}
