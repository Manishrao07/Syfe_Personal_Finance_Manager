package com.syfe.financemanager.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A savings goal with its progress fields computed fresh at response time. */
@Getter
@Builder
@AllArgsConstructor
public class GoalResponse {
    private final Long id;
    private final String goalName;
    private final BigDecimal targetAmount;
    private final LocalDate targetDate;
    private final LocalDate startDate;
    private final BigDecimal currentProgress;
    private final BigDecimal progressPercentage;
    private final BigDecimal remainingAmount;
}
