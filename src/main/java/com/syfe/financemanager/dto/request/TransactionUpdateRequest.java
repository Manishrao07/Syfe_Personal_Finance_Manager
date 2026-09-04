package com.syfe.financemanager.dto.request;

import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Payload for {@code PUT /api/transactions/{id}}. Every transaction field is
 * editable except {@code date} — this DTO intentionally has no {@code date}
 * property, so a {@code date} sent by the client is silently ignored by Jackson
 * rather than rejected (see README "Assumptions").
 */
@Getter
@Setter
public class TransactionUpdateRequest {

    @Positive(message = "amount must be positive")
    private BigDecimal amount;

    private String category;

    private String description;
}
