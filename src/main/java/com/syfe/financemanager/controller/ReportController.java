package com.syfe.financemanager.controller;

import com.syfe.financemanager.dto.response.MonthlyReportResponse;
import com.syfe.financemanager.dto.response.YearlyReportResponse;
import com.syfe.financemanager.security.SecurityUtils;
import com.syfe.financemanager.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Monthly and yearly income/expense reports for the authenticated user. */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/monthly/{year}/{month}")
    public ResponseEntity<MonthlyReportResponse> getMonthlyReport(@PathVariable int year, @PathVariable int month) {
        return ResponseEntity.ok(reportService.getMonthlyReport(SecurityUtils.getCurrentUserId(), year, month));
    }

    @GetMapping("/yearly/{year}")
    public ResponseEntity<YearlyReportResponse> getYearlyReport(@PathVariable int year) {
        return ResponseEntity.ok(reportService.getYearlyReport(SecurityUtils.getCurrentUserId(), year));
    }
}
