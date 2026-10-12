package com.expensetracker.budget;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonthlyBudgetRepository extends JpaRepository<MonthlyBudget, Long> {

    Optional<MonthlyBudget> findByUserIdAndMonthAndYear(Long userId, int month, int year);

    Optional<MonthlyBudget> findByIdAndUserId(Long id, Long userId);

    List<MonthlyBudget> findAllByUserIdOrderByYearDescMonthDesc(Long userId);

    boolean existsByUserIdAndMonthAndYear(Long userId, int month, int year);
}
