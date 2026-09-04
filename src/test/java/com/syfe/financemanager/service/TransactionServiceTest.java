package com.syfe.financemanager.service;

import com.syfe.financemanager.dto.request.TransactionRequest;
import com.syfe.financemanager.dto.request.TransactionUpdateRequest;
import com.syfe.financemanager.dto.response.TransactionResponse;
import com.syfe.financemanager.entity.Category;
import com.syfe.financemanager.entity.Transaction;
import com.syfe.financemanager.entity.TransactionType;
import com.syfe.financemanager.entity.User;
import com.syfe.financemanager.exception.BadRequestException;
import com.syfe.financemanager.exception.ResourceNotFoundException;
import com.syfe.financemanager.repository.TransactionRepository;
import com.syfe.financemanager.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private TransactionService transactionService;

    private static final Long USER_ID = 1L;

    private Category salaryCategory() {
        return Category.builder().id(1L).name("Salary").type(TransactionType.INCOME).custom(false).build();
    }

    @Test
    void create_savesTransaction_withTypeDerivedFromCategory() {
        TransactionRequest request = new TransactionRequest();
        request.setAmount(new BigDecimal("50000.00"));
        request.setDate(LocalDate.now().minusDays(1));
        request.setCategory("Salary");
        request.setDescription("January Salary");

        when(categoryService.resolveVisibleCategory(USER_ID, "Salary")).thenReturn(salaryCategory());
        when(userRepository.getReferenceById(USER_ID)).thenReturn(User.builder().id(USER_ID).build());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        TransactionResponse response = transactionService.create(USER_ID, request);

        assertThat(response.getType()).isEqualTo(TransactionType.INCOME);
        assertThat(response.getCategory()).isEqualTo("Salary");
    }

    @Test
    void create_throwsBadRequest_whenDateIsInTheFuture() {
        TransactionRequest request = new TransactionRequest();
        request.setAmount(BigDecimal.TEN);
        request.setDate(LocalDate.now().plusDays(1));
        request.setCategory("Salary");

        assertThatThrownBy(() -> transactionService.create(USER_ID, request))
                .isInstanceOf(BadRequestException.class);

        verifyNoInteractions(transactionRepository);
    }

    @Test
    void create_propagatesBadRequest_whenCategoryNotVisible() {
        TransactionRequest request = new TransactionRequest();
        request.setAmount(BigDecimal.TEN);
        request.setDate(LocalDate.now());
        request.setCategory("Nonexistent");

        when(categoryService.resolveVisibleCategory(USER_ID, "Nonexistent"))
                .thenThrow(new BadRequestException("Category not visible"));

        assertThatThrownBy(() -> transactionService.create(USER_ID, request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void update_updatesEditableFields_butNeverDate() {
        Transaction existing = Transaction.builder()
                .id(5L).user(User.builder().id(USER_ID).build()).category(salaryCategory())
                .amount(new BigDecimal("100.00")).date(LocalDate.of(2024, 1, 15))
                .description("old").type(TransactionType.INCOME).deleted(false).build();

        when(transactionRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(existing));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        TransactionUpdateRequest request = new TransactionUpdateRequest();
        request.setAmount(new BigDecimal("200.00"));
        request.setDescription("updated");

        TransactionResponse response = transactionService.update(USER_ID, 5L, request);

        assertThat(response.getAmount()).isEqualByComparingTo("200.00");
        assertThat(response.getDescription()).isEqualTo("updated");
        assertThat(response.getDate()).isEqualTo(LocalDate.of(2024, 1, 15));
        verifyNoInteractions(categoryService);
    }

    @Test
    void update_throwsNotFound_whenTransactionMissing() {
        when(transactionRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.update(USER_ID, 99L, new TransactionUpdateRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_throwsNotFound_whenTransactionBelongsToAnotherUser() {
        Transaction existing = Transaction.builder()
                .id(5L).user(User.builder().id(2L).build()).category(salaryCategory())
                .amount(BigDecimal.TEN).date(LocalDate.now()).type(TransactionType.INCOME).deleted(false).build();
        when(transactionRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> transactionService.update(USER_ID, 5L, new TransactionUpdateRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_softDeletesTransaction() {
        Transaction existing = Transaction.builder()
                .id(5L).user(User.builder().id(USER_ID).build()).category(salaryCategory())
                .amount(BigDecimal.TEN).date(LocalDate.now()).type(TransactionType.INCOME).deleted(false).build();
        when(transactionRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(existing));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        transactionService.delete(USER_ID, 5L);

        assertThat(existing.isDeleted()).isTrue();
        verify(transactionRepository).save(existing);
    }

    @Test
    void delete_throwsNotFound_whenTransactionBelongsToAnotherUser() {
        Transaction existing = Transaction.builder()
                .id(5L).user(User.builder().id(2L).build()).category(salaryCategory())
                .amount(BigDecimal.TEN).date(LocalDate.now()).type(TransactionType.INCOME).deleted(false).build();
        when(transactionRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> transactionService.delete(USER_ID, 5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
