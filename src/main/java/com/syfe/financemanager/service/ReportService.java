package com.syfe.financemanager.service;

import com.syfe.financemanager.dto.response.MonthlyReportResponse;
import com.syfe.financemanager.dto.response.YearlyReportResponse;
import com.syfe.financemanager.entity.TransactionType;
import com.syfe.financemanager.exception.BadRequestException;
import com.syfe.financemanager.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Year;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds monthly/yearly income-vs-expense breakdowns by category. Categories with
 * zero activity in the period are omitted from the maps, matching the spec's
 * example payloads.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final TransactionRepository transactionRepository;

    public MonthlyReportResponse getMonthlyReport(Long userId, int year, int month) {
        if (month < 1 || month > 12) {
            throw new BadRequestException("month must be between 1 and 12");
        }
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        Map<String, BigDecimal> income = groupedTotals(userId, TransactionType.INCOME, startDate, endDate);
        Map<String, BigDecimal> expenses = groupedTotals(userId, TransactionType.EXPENSE, startDate, endDate);

        return MonthlyReportResponse.builder()
                .month(month)
                .year(year)
                .totalIncome(income)
                .totalExpenses(expenses)
                .netSavings(netSavings(income, expenses))
                .build();
    }

    public YearlyReportResponse getYearlyReport(Long userId, int year) {
        LocalDate startDate = Year.of(year).atDay(1);
        LocalDate endDate = Year.of(year).atMonth(12).atEndOfMonth();

        Map<String, BigDecimal> income = groupedTotals(userId, TransactionType.INCOME, startDate, endDate);
        Map<String, BigDecimal> expenses = groupedTotals(userId, TransactionType.EXPENSE, startDate, endDate);

        return YearlyReportResponse.builder()
                .year(year)
                .totalIncome(income)
                .totalExpenses(expenses)
                .netSavings(netSavings(income, expenses))
                .build();
    }

    private Map<String, BigDecimal> groupedTotals(Long userId, TransactionType type, LocalDate startDate, LocalDate endDate) {
        List<TransactionRepository.CategoryTotal> rows =
                transactionRepository.sumByUserAndTypeBetweenGroupedByCategory(userId, type, startDate, endDate);

        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (TransactionRepository.CategoryTotal row : rows) {
            result.put(row.getCategoryName(), row.getTotal().setScale(2, RoundingMode.HALF_UP));
        }
        return result;
    }

    private BigDecimal netSavings(Map<String, BigDecimal> income, Map<String, BigDecimal> expenses) {
        BigDecimal totalIncome = income.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpenses = expenses.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return totalIncome.subtract(totalExpenses);
    }
}
