package com.syfe.financemanager.service;

import com.syfe.financemanager.dto.request.TransactionRequest;
import com.syfe.financemanager.dto.request.TransactionUpdateRequest;
import com.syfe.financemanager.dto.response.MessageResponse;
import com.syfe.financemanager.dto.response.TransactionListResponse;
import com.syfe.financemanager.dto.response.TransactionResponse;
import com.syfe.financemanager.entity.Category;
import com.syfe.financemanager.entity.Transaction;
import com.syfe.financemanager.entity.TransactionType;
import com.syfe.financemanager.exception.BadRequestException;
import com.syfe.financemanager.exception.ResourceNotFoundException;
import com.syfe.financemanager.repository.TransactionRepository;
import com.syfe.financemanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Transaction CRUD with soft delete. Note: the transaction endpoints' documented
 * status codes are 400/401/404 only (no 403) — so, unlike goals, a transaction that
 * exists but belongs to another user is reported as 404, not 403, to avoid leaking
 * whether the id exists at all. This is called out in the README.
 */
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final CategoryService categoryService;

    /** Creates a transaction; its type is derived from the referenced category, and its date cannot be in the future. */
    @Transactional
    public TransactionResponse create(Long userId, TransactionRequest request) {
        if (request.getDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("date cannot be in the future");
        }

        Category category = categoryService.resolveVisibleCategory(userId, request.getCategory());

        Transaction transaction = Transaction.builder()
                .user(userRepository.getReferenceById(userId))
                .category(category)
                .amount(request.getAmount())
                .date(request.getDate())
                .description(request.getDescription())
                .type(category.getType())
                .deleted(false)
                .build();

        return toResponse(transactionRepository.save(transaction));
    }

    /**
     * Lists the given user's non-deleted transactions, newest first, optionally filtered by
     * date range, category (by id or name — {@code categoryId} takes priority if both are
     * given), and type. Read-only and transactional so the category name is safe to read
     * off each result even when {@code open-in-view} is disabled (see README).
     */
    @Transactional(readOnly = true)
    public TransactionListResponse getTransactions(Long userId, LocalDate startDate, LocalDate endDate,
                                                     Long categoryId, String categoryName, TransactionType type) {
        Long effectiveCategoryId = categoryId != null
                ? categoryId
                : categoryName != null ? categoryService.resolveVisibleCategory(userId, categoryName).getId() : null;

        List<TransactionResponse> transactions = transactionRepository
                .search(userId, startDate, endDate, effectiveCategoryId, type).stream()
                .map(this::toResponse)
                .toList();
        return new TransactionListResponse(transactions);
    }

    /** Updates any editable field of one of the given user's own transactions; {@code date} is not editable. */
    @Transactional
    public TransactionResponse update(Long userId, Long id, TransactionUpdateRequest request) {
        Transaction transaction = findOwned(userId, id);

        if (request.getCategory() != null) {
            Category category = categoryService.resolveVisibleCategory(userId, request.getCategory());
            transaction.setCategory(category);
            transaction.setType(category.getType());
        }
        if (request.getAmount() != null) {
            transaction.setAmount(request.getAmount());
        }
        if (request.getDescription() != null) {
            transaction.setDescription(request.getDescription());
        }

        return toResponse(transactionRepository.save(transaction));
    }

    /** Soft-deletes one of the given user's own transactions; it is excluded from listings, goals, and reports thereafter. */
    @Transactional
    public MessageResponse delete(Long userId, Long id) {
        Transaction transaction = findOwned(userId, id);
        transaction.setDeleted(true);
        transactionRepository.save(transaction);
        return new MessageResponse("Transaction deleted successfully");
    }

    private Transaction findOwned(Long userId, Long id) {
        Transaction transaction = transactionRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        if (!transaction.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Transaction not found");
        }
        return transaction;
    }

    private TransactionResponse toResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .amount(transaction.getAmount())
                .date(transaction.getDate())
                .category(transaction.getCategory().getName())
                .description(transaction.getDescription())
                .type(transaction.getType())
                .build();
    }
}
