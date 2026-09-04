package com.syfe.financemanager.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Payload for {@code POST /api/transactions}. */
@Getter
@Setter
public class TransactionRequest {

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "date is required")
    private LocalDate date;

    @NotBlank(message = "category is required")
    private String category;

    private String description;
}
