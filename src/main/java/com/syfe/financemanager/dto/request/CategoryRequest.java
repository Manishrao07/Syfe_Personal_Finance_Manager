package com.syfe.financemanager.dto.request;

import com.syfe.financemanager.entity.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** Payload for {@code POST /api/categories}. */
@Getter
@Setter
public class CategoryRequest {

    @NotBlank(message = "name is required")
    private String name;

    @NotNull(message = "type is required")
    private TransactionType type;
}
