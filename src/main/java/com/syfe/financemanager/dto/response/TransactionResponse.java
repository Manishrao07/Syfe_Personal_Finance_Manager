package com.syfe.financemanager.dto.response;

import com.syfe.financemanager.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A single transaction as returned to clients. */
@Getter
@Builder
@AllArgsConstructor
public class TransactionResponse {
    private final Long id;
    private final BigDecimal amount;
    private final LocalDate date;
    private final String category;
    private final String description;
    private final TransactionType type;
}
