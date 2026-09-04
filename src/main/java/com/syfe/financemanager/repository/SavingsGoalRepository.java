package com.syfe.financemanager.repository;

import com.syfe.financemanager.entity.SavingsGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Data access for {@link SavingsGoal}. */
public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {

    List<SavingsGoal> findByUserId(Long userId);
}
