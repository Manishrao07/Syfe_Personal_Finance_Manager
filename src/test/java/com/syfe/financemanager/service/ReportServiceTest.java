package com.syfe.financemanager.service;

import com.syfe.financemanager.dto.response.MonthlyReportResponse;
import com.syfe.financemanager.dto.response.YearlyReportResponse;
import com.syfe.financemanager.entity.TransactionType;
import com.syfe.financemanager.exception.BadRequestException;
import com.syfe.financemanager.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private ReportService reportService;

    private static final Long USER_ID = 1L;

    private record CategoryTotalStub(String categoryName, BigDecimal total)
            implements TransactionRepository.CategoryTotal {
        @Override
        public String getCategoryName() {
            return categoryName;
        }

        @Override
        public BigDecimal getTotal() {
            return total;
        }
    }

    @Test
    void getMonthlyReport_groupsByCategoryAndComputesNetSavings() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);

        when(transactionRepository.sumByUserAndTypeBetweenGroupedByCategory(USER_ID, TransactionType.INCOME, start, end))
                .thenReturn(List.of(new CategoryTotalStub("Salary", new BigDecimal("3000.00")),
                        new CategoryTotalStub("Freelance", new BigDecimal("500.00"))));
        when(transactionRepository.sumByUserAndTypeBetweenGroupedByCategory(USER_ID, TransactionType.EXPENSE, start, end))
                .thenReturn(List.of(new CategoryTotalStub("Food", new BigDecimal("400.00")),
                        new CategoryTotalStub("Rent", new BigDecimal("1200.00")),
                        new CategoryTotalStub("Transportation", new BigDecimal("200.00"))));

        MonthlyReportResponse response = reportService.getMonthlyReport(USER_ID, 2024, 1);

        assertThat(response.getTotalIncome()).containsEntry("Salary", new BigDecimal("3000.00"));
        assertThat(response.getTotalExpenses()).containsEntry("Rent", new BigDecimal("1200.00"));
        assertThat(response.getNetSavings()).isEqualByComparingTo("1700.00");
        assertThat(response.getTotalIncome()).doesNotContainKey("Healthcare");
    }

    @Test
    void getMonthlyReport_throwsBadRequest_forOutOfRangeMonth() {
        assertThatThrownBy(() -> reportService.getMonthlyReport(USER_ID, 2024, 13))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> reportService.getMonthlyReport(USER_ID, 2024, 0))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void getYearlyReport_aggregatesAcrossWholeYear() {
        when(transactionRepository.sumByUserAndTypeBetweenGroupedByCategory(eq(USER_ID), eq(TransactionType.INCOME), any(), any()))
                .thenReturn(List.of(new CategoryTotalStub("Salary", new BigDecimal("36000.00"))));
        when(transactionRepository.sumByUserAndTypeBetweenGroupedByCategory(eq(USER_ID), eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(List.of(new CategoryTotalStub("Rent", new BigDecimal("14400.00"))));

        YearlyReportResponse response = reportService.getYearlyReport(USER_ID, 2024);

        assertThat(response.getYear()).isEqualTo(2024);
        assertThat(response.getNetSavings()).isEqualByComparingTo("21600.00");
    }
}
