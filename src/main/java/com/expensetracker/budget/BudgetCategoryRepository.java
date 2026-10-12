package com.expensetracker.budget;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BudgetCategoryRepository extends JpaRepository<BudgetCategory, Long> {

    List<BudgetCategory> findByMonthlyBudgetId(Long monthlyBudgetId);

    void deleteByMonthlyBudgetId(Long monthlyBudgetId);

    void deleteByCategoryId(Long categoryId);
}
