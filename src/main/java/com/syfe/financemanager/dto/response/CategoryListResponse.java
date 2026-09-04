package com.syfe.financemanager.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/** Response body for {@code GET /api/categories}. */
@Getter
@AllArgsConstructor
public class CategoryListResponse {
    private final List<CategoryResponse> categories;
}
