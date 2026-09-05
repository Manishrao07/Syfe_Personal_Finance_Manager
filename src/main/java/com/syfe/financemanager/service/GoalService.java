package com.syfe.financemanager.service;

import com.syfe.financemanager.dto.request.GoalRequest;
import com.syfe.financemanager.dto.request.GoalUpdateRequest;
import com.syfe.financemanager.dto.response.GoalListResponse;
import com.syfe.financemanager.dto.response.GoalResponse;
import com.syfe.financemanager.dto.response.MessageResponse;
import com.syfe.financemanager.entity.SavingsGoal;
import com.syfe.financemanager.entity.TransactionType;
import com.syfe.financemanager.exception.BadRequestException;
import com.syfe.financemanager.exception.ForbiddenException;
import com.syfe.financemanager.exception.ResourceNotFoundException;
import com.syfe.financemanager.repository.SavingsGoalRepository;
import com.syfe.financemanager.repository.TransactionRepository;
import com.syfe.financemanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Savings-goal CRUD. Progress is never persisted — {@link #toResponse} recomputes
 * {@code currentProgress}, {@code progressPercentage}, and {@code remainingAmount}
 * from the user's transactions on every call, per the spec's "computed fresh on
 * every GET" requirement.
 */
@Service
@RequiredArgsConstructor
public class GoalService {

    private final SavingsGoalRepository savingsGoalRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Transactional
    public GoalResponse create(Long userId, GoalRequest request) {
        if (!request.getTargetDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("targetDate must be strictly in the future");
        }

        LocalDate startDate = request.getStartDate() != null ? request.getStartDate() : LocalDate.now();

        SavingsGoal goal = SavingsGoal.builder()
                .user(userRepository.getReferenceById(userId))
                .goalName(request.getGoalName())
                .targetAmount(request.getTargetAmount())
                .targetDate(request.getTargetDate())
                .startDate(startDate)
                .build();

        return toResponse(savingsGoalRepository.save(goal));
    }

    public GoalListResponse getAll(Long userId) {
        List<GoalResponse> goals = savingsGoalRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
        return new GoalListResponse(goals);
    }

    public GoalResponse getOne(Long userId, Long id) {
        return toResponse(findOwned(userId, id));
    }

    @Transactional
    public GoalResponse update(Long userId, Long id, GoalUpdateRequest request) {
        SavingsGoal goal = findOwned(userId, id);

        if (request.getTargetDate() != null) {
            if (!request.getTargetDate().isAfter(LocalDate.now())) {
                throw new BadRequestException("targetDate must be strictly in the future");
            }
            goal.setTargetDate(request.getTargetDate());
        }
        if (request.getTargetAmount() != null) {
            goal.setTargetAmount(request.getTargetAmount());
        }

        return toResponse(savingsGoalRepository.save(goal));
    }

    @Transactional
    public MessageResponse delete(Long userId, Long id) {
        SavingsGoal goal = findOwned(userId, id);
        savingsGoalRepository.delete(goal);
        return new MessageResponse("Goal deleted successfully");
    }

    private SavingsGoal findOwned(Long userId, Long id) {
        SavingsGoal goal = savingsGoalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found"));
        if (!goal.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You do not have permission to access this goal");
        }
        return goal;
    }

    private GoalResponse toResponse(SavingsGoal goal) {
        Long userId = goal.getUser().getId();
        BigDecimal income = transactionRepository.sumByUserAndTypeSince(userId, TransactionType.INCOME, goal.getStartDate());
        BigDecimal expense = transactionRepository.sumByUserAndTypeSince(userId, TransactionType.EXPENSE, goal.getStartDate());
        BigDecimal currentProgress = income.subtract(expense);

        double percentage = goal.getTargetAmount().signum() == 0
                ? 0.0
                : Math.round(currentProgress.doubleValue() / goal.getTargetAmount().doubleValue() * 10000.0) / 100.0;

        BigDecimal remaining = goal.getTargetAmount().subtract(currentProgress);

        return GoalResponse.builder()
                .id(goal.getId())
                .goalName(goal.getGoalName())
                .targetAmount(goal.getTargetAmount())
                .targetDate(goal.getTargetDate())
                .startDate(goal.getStartDate())
                .currentProgress(currentProgress)
                .progressPercentage(percentage)
                .remainingAmount(remaining)
                .build();
    }
}
