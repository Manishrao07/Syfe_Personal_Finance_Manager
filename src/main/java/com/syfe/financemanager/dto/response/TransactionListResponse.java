package com.syfe.financemanager.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/** Response body for {@code GET /api/transactions}. */
@Getter
@AllArgsConstructor
public class TransactionListResponse {
    private final List<TransactionResponse> transactions;
}
