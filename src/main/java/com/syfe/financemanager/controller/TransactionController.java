package com.syfe.financemanager.controller;

import com.syfe.financemanager.dto.request.TransactionRequest;
import com.syfe.financemanager.dto.request.TransactionUpdateRequest;
import com.syfe.financemanager.dto.response.MessageResponse;
import com.syfe.financemanager.dto.response.TransactionListResponse;
import com.syfe.financemanager.dto.response.TransactionResponse;
import com.syfe.financemanager.entity.TransactionType;
import com.syfe.financemanager.security.SecurityUtils;
import com.syfe.financemanager.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/** CRUD for the authenticated user's income/expense transactions. */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    /** Creates an income or expense transaction; its type is derived from the referenced category. */
    @PostMapping
    public ResponseEntity<TransactionResponse> create(@Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = transactionService.create(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Lists the caller's non-deleted transactions, newest first, optionally filtered. */
    @GetMapping
    public ResponseEntity<TransactionListResponse> getTransactions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) TransactionType type) {
        TransactionListResponse response = transactionService.getTransactions(
                SecurityUtils.getCurrentUserId(), startDate, endDate, categoryId, category, type);
        return ResponseEntity.ok(response);
    }

    /** Updates any editable field of one of the caller's own transactions; {@code date} cannot be changed. */
    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> update(@PathVariable Long id,
                                                        @Valid @RequestBody TransactionUpdateRequest request) {
        return ResponseEntity.ok(transactionService.update(SecurityUtils.getCurrentUserId(), id, request));
    }

    /** Soft-deletes one of the caller's own transactions; it is excluded from listings, goals, and reports thereafter. */
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> delete(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.delete(SecurityUtils.getCurrentUserId(), id));
    }
}
