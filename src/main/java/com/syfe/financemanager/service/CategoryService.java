package com.syfe.financemanager.service;

import com.syfe.financemanager.dto.request.CategoryRequest;
import com.syfe.financemanager.dto.response.CategoryListResponse;
import com.syfe.financemanager.dto.response.CategoryResponse;
import com.syfe.financemanager.dto.response.MessageResponse;
import com.syfe.financemanager.entity.Category;
import com.syfe.financemanager.exception.BadRequestException;
import com.syfe.financemanager.exception.ConflictException;
import com.syfe.financemanager.exception.ForbiddenException;
import com.syfe.financemanager.exception.ResourceNotFoundException;
import com.syfe.financemanager.repository.CategoryRepository;
import com.syfe.financemanager.repository.TransactionRepository;
import com.syfe.financemanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Category visibility, creation, and deletion. Default categories are global
 * ({@code owner == null}); custom categories are scoped to their owning user and
 * unique only within that user's own set.
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public CategoryListResponse getVisibleCategories(Long userId) {
        List<CategoryResponse> categories = categoryRepository.findByOwnerIsNullOrOwnerId(userId).stream()
                .map(this::toResponse)
                .toList();
        return new CategoryListResponse(categories);
    }

    @Transactional
    public CategoryResponse createCustomCategory(Long userId, CategoryRequest request) {
        if (categoryRepository.existsByNameAndOwnerId(request.getName(), userId)) {
            throw new ConflictException("A category named '" + request.getName() + "' already exists");
        }

        Category category = Category.builder()
                .name(request.getName())
                .type(request.getType())
                .custom(true)
                .owner(userRepository.getReferenceById(userId))
                .build();

        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public MessageResponse deleteCategory(Long userId, String name) {
        Optional<Category> ownCategory = categoryRepository.findByNameAndOwnerId(name, userId);
        if (ownCategory.isPresent()) {
            Category category = ownCategory.get();
            if (transactionRepository.existsByCategoryId(category.getId())) {
                throw new ConflictException("Category is referenced by existing transactions and cannot be deleted");
            }
            categoryRepository.delete(category);
            return new MessageResponse("Category deleted successfully");
        }

        if (categoryRepository.findByNameAndOwnerIsNull(name).isPresent()) {
            throw new BadRequestException("Default categories cannot be deleted");
        }

        // Not this user's own category and not a default: is it another user's custom category?
        if (categoryRepository.existsByNameAndOwnerIsNotNull(name)) {
            throw new ForbiddenException("You do not have permission to delete this category");
        }

        throw new ResourceNotFoundException("Category not found: " + name);
    }

    /** Resolves a category name to a {@link Category} visible to the given user, or throws 400. */
    public Category resolveVisibleCategory(Long userId, String name) {
        return categoryRepository.findByNameAndOwnerId(name, userId)
                .or(() -> categoryRepository.findByNameAndOwnerIsNull(name))
                .orElseThrow(() -> new BadRequestException("Category '" + name + "' does not exist or is not accessible to you"));
    }

    private CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .name(category.getName())
                .type(category.getType())
                .custom(category.isCustom())
                .build();
    }
}
