package com.syfe.financemanager.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Map;

/** Response body for {@code GET /api/reports/monthly/{year}/{month}}. */
@Getter
@Builder
@AllArgsConstructor
public class MonthlyReportResponse {
    private final int month;
    private final int year;
    private final Map<String, BigDecimal> totalIncome;
    private final Map<String, BigDecimal> totalExpenses;
    private final BigDecimal netSavings;
}
