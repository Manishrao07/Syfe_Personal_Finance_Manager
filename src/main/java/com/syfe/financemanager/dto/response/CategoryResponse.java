package com.syfe.financemanager.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.syfe.financemanager.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** A single category as returned to clients. */
@Getter
@Builder
@AllArgsConstructor
public class CategoryResponse {
    private final String name;
    private final TransactionType type;

    @JsonProperty("isCustom")
    private final boolean custom;
}
