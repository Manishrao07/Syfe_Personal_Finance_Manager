package com.syfe.financemanager.controller;

import com.syfe.financemanager.dto.request.CategoryRequest;
import com.syfe.financemanager.dto.response.CategoryListResponse;
import com.syfe.financemanager.dto.response.CategoryResponse;
import com.syfe.financemanager.dto.response.MessageResponse;
import com.syfe.financemanager.security.SecurityUtils;
import com.syfe.financemanager.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Category listing, custom-category creation, and custom-category deletion. */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /** Returns the global default categories plus the caller's own custom categories. */
    @GetMapping
    public ResponseEntity<CategoryListResponse> getCategories() {
        return ResponseEntity.ok(categoryService.getVisibleCategories(SecurityUtils.getCurrentUserId()));
    }

    /** Creates a custom category owned by the caller. */
    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.createCustomCategory(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Deletes one of the caller's own custom categories, by name. */
    @DeleteMapping("/{name}")
    public ResponseEntity<MessageResponse> deleteCategory(@PathVariable String name) {
        return ResponseEntity.ok(categoryService.deleteCategory(SecurityUtils.getCurrentUserId(), name));
    }
}
