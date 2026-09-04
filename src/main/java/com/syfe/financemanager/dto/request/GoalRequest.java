package com.syfe.financemanager.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Payload for {@code POST /api/goals}. */
@Getter
@Setter
public class GoalRequest {

    @NotBlank(message = "goalName is required")
    private String goalName;

    @NotNull(message = "targetAmount is required")
    @Positive(message = "targetAmount must be positive")
    private BigDecimal targetAmount;

    @NotNull(message = "targetDate is required")
    private LocalDate targetDate;

    /** Optional; defaults to the creation date when omitted. */
    private LocalDate startDate;
}
