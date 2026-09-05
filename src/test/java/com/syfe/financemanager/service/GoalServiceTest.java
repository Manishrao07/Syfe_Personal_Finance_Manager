package com.syfe.financemanager.service;

import com.syfe.financemanager.dto.request.GoalRequest;
import com.syfe.financemanager.dto.request.GoalUpdateRequest;
import com.syfe.financemanager.dto.response.GoalResponse;
import com.syfe.financemanager.entity.SavingsGoal;
import com.syfe.financemanager.entity.TransactionType;
import com.syfe.financemanager.entity.User;
import com.syfe.financemanager.exception.BadRequestException;
import com.syfe.financemanager.exception.ForbiddenException;
import com.syfe.financemanager.exception.ResourceNotFoundException;
import com.syfe.financemanager.repository.SavingsGoalRepository;
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
class GoalServiceTest {

    @Mock
    private SavingsGoalRepository savingsGoalRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GoalService goalService;

    private static final Long USER_ID = 1L;

    private SavingsGoal existingGoal() {
        return SavingsGoal.builder()
                .id(1L)
                .user(User.builder().id(USER_ID).build())
                .goalName("Emergency Fund")
                .targetAmount(new BigDecimal("5000.00"))
                .targetDate(LocalDate.now().plusMonths(6))
                .startDate(LocalDate.now().minusMonths(1))
                .build();
    }

    @Test
    void create_computesProgressFromNetCashFlowSinceStartDate() {
        GoalRequest request = new GoalRequest();
        request.setGoalName("Emergency Fund");
        request.setTargetAmount(new BigDecimal("5000.00"));
        request.setTargetDate(LocalDate.now().plusYears(1));

        when(userRepository.getReferenceById(USER_ID)).thenReturn(User.builder().id(USER_ID).build());
        when(savingsGoalRepository.save(any(SavingsGoal.class))).thenAnswer(inv -> {
            SavingsGoal g = inv.getArgument(0);
            g.setId(1L);
            return g;
        });
        when(transactionRepository.sumByUserAndTypeSince(eq(USER_ID), eq(TransactionType.INCOME), any()))
                .thenReturn(new BigDecimal("2000.00"));
        when(transactionRepository.sumByUserAndTypeSince(eq(USER_ID), eq(TransactionType.EXPENSE), any()))
                .thenReturn(new BigDecimal("1000.00"));

        GoalResponse response = goalService.create(USER_ID, request);

        assertThat(response.getCurrentProgress()).isEqualByComparingTo("1000.00");
        assertThat(response.getProgressPercentage()).isEqualTo(20.0);
        assertThat(response.getRemainingAmount()).isEqualByComparingTo("4000.00");
        assertThat(response.getStartDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void create_throwsBadRequest_whenTargetDateIsNotStrictlyFuture() {
        GoalRequest request = new GoalRequest();
        request.setGoalName("Emergency Fund");
        request.setTargetAmount(BigDecimal.TEN);
        request.setTargetDate(LocalDate.now());

        assertThatThrownBy(() -> goalService.create(USER_ID, request))
                .isInstanceOf(BadRequestException.class);

        verifyNoInteractions(savingsGoalRepository);
    }

    @Test
    void create_throwsBadRequest_whenStartDateIsNotBeforeTargetDate() {
        GoalRequest request = new GoalRequest();
        request.setGoalName("Invalid Dates Goal");
        request.setTargetAmount(BigDecimal.TEN);
        request.setTargetDate(LocalDate.now().plusYears(1));
        request.setStartDate(LocalDate.now().plusYears(2));

        assertThatThrownBy(() -> goalService.create(USER_ID, request))
                .isInstanceOf(BadRequestException.class);

        verifyNoInteractions(savingsGoalRepository);
    }

    @Test
    void getOne_throwsNotFound_whenGoalMissing() {
        when(savingsGoalRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> goalService.getOne(USER_ID, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getOne_throwsForbidden_whenGoalBelongsToAnotherUser() {
        SavingsGoal goal = existingGoal();
        goal.setUser(User.builder().id(2L).build());
        when(savingsGoalRepository.findById(1L)).thenReturn(Optional.of(goal));

        assertThatThrownBy(() -> goalService.getOne(USER_ID, 1L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void update_recomputesPercentageAgainstNewTargetAmount() {
        SavingsGoal goal = existingGoal();
        when(savingsGoalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(savingsGoalRepository.save(any(SavingsGoal.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.sumByUserAndTypeSince(eq(USER_ID), eq(TransactionType.INCOME), any()))
                .thenReturn(new BigDecimal("1000.00"));
        when(transactionRepository.sumByUserAndTypeSince(eq(USER_ID), eq(TransactionType.EXPENSE), any()))
                .thenReturn(BigDecimal.ZERO);

        GoalUpdateRequest request = new GoalUpdateRequest();
        request.setTargetAmount(new BigDecimal("6000.00"));

        GoalResponse response = goalService.update(USER_ID, 1L, request);

        assertThat(response.getTargetAmount()).isEqualByComparingTo("6000.00");
        assertThat(response.getCurrentProgress()).isEqualByComparingTo("1000.00");
        assertThat(response.getProgressPercentage()).isEqualTo(16.67);
    }

    @Test
    void update_throwsBadRequest_whenNewTargetDateNotInFuture() {
        SavingsGoal goal = existingGoal();
        when(savingsGoalRepository.findById(1L)).thenReturn(Optional.of(goal));

        GoalUpdateRequest request = new GoalUpdateRequest();
        request.setTargetDate(LocalDate.now().minusDays(1));

        assertThatThrownBy(() -> goalService.update(USER_ID, 1L, request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void delete_removesOwnGoal() {
        SavingsGoal goal = existingGoal();
        when(savingsGoalRepository.findById(1L)).thenReturn(Optional.of(goal));

        goalService.delete(USER_ID, 1L);

        verify(savingsGoalRepository).delete(goal);
    }

    @Test
    void delete_throwsForbidden_whenGoalBelongsToAnotherUser() {
        SavingsGoal goal = existingGoal();
        goal.setUser(User.builder().id(2L).build());
        when(savingsGoalRepository.findById(1L)).thenReturn(Optional.of(goal));

        assertThatThrownBy(() -> goalService.delete(USER_ID, 1L))
                .isInstanceOf(ForbiddenException.class);

        verify(savingsGoalRepository, never()).delete(any());
    }
}
